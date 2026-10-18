package multiroute;

import com.jcraft.jsch.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Gateway-bound remote directory browsing and verified FTP/FTPS/SFTP publication. */
final class RemoteFiles {
    private RemoteFiles() { }
    static URI uri(String text, boolean file) throws IOException {
        URI value;
        try { value = URI.create(text.trim()).normalize(); }
        catch (RuntimeException malformed) { throw new IOException("Invalid source URL"); }
        String scheme = value.getScheme() == null ? "" : value.getScheme().toLowerCase(Locale.ROOT);
        if (!Set.of("ftp", "ftps", "sftp").contains(scheme) || value.getHost() == null || value.getRawUserInfo() != null ||
                value.getRawQuery() != null || value.getRawFragment() != null || value.getPort() == 0 || value.getPort() > 65535 ||
                value.getPath() == null || value.getPath().contains("\\") || value.getPath().chars().anyMatch(c -> c < 32 || c == 127))
            throw new IOException("Invalid remote FTP or SFTP URL");
        if (file && (value.getPath().isEmpty() || value.getPath().endsWith("/"))) throw new IOException("Use a source URL pointing to a file");
        return value;
    }
    private static void gateway(Map<String,Object> route) throws IOException {
        if (route == null || !"gateway".equals(route.get("type")))
            throw new Transport.NonRetryableException("This source requires a selected application gateway");
    }
    static Map<String,Object> list(Map<String,Object> route, String url) throws IOException, InterruptedException {
        gateway(route); URI uri = uri(url, false);
        List<Map<String,Object>> entries = uri.getScheme().equalsIgnoreCase("sftp") ? sftpList(route, uri) : ftpList(route, uri);
        entries.sort(Comparator.<Map<String,Object>,Boolean>comparing(item -> !Boolean.TRUE.equals(item.get("directory")))
                .thenComparing(item -> String.valueOf(item.get("name")), String.CASE_INSENSITIVE_ORDER));
        return Map.of("url", uri.toASCIIString(), "entries", entries);
    }
    static Map<String,Object> upload(Map<String,Object> route, String url, Path local, String temporary,
            long size, String sha256, boolean overwrite, SourceTransfer.UploadControl control) throws IOException, InterruptedException {
        gateway(route); URI uri = uri(url, true); control.check();
        if (!temporary.startsWith(parent(uri.getPath())) || !temporary.contains(".multiroute-upload-") ||
                temporary.chars().anyMatch(c -> c < 32 || c == 127)) throw new IOException("Invalid remote partial file");
        if (uri.getScheme().equalsIgnoreCase("sftp")) sftpUpload(route, uri, local, temporary, size, sha256, overwrite, control);
        else ftpUpload(route, uri, local, temporary, size, sha256, overwrite, control);
        return Map.of("uploaded_bytes", size, "sha256", sha256, "verification", "sha256-readback", "verified", true);
    }
    static String parent(String path) { int slash = path.lastIndexOf('/'); return slash < 0 ? "/" : path.substring(0, slash + 1); }
    static String name(String path) { return path.substring(path.lastIndexOf('/') + 1); }
    static String literal(String path) { return path.replace("*", "\\*").replace("?", "\\?"); }
    private static boolean nameSafe(String name) {
        return !name.isEmpty() && !name.equals(".") && !name.equals("..") && !name.contains("/") && !name.contains("\\") &&
                name.chars().noneMatch(c -> c < 32 || c == 127);
    }
    private static Map<String,Object> entry(URI uri, String name, boolean directory, long size, String modified) throws IOException {
        String base = uri.getPath().isEmpty() ? "/" : uri.getPath(); if (!base.endsWith("/")) base += "/";
        String path = base + name;
        try {
            return Map.of("name", name, "path", path, "url", new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), path + (directory ? "/" : ""), null, null).toASCIIString(),
                    "directory", directory, "size", directory ? 0L : size, "modified", modified);
        } catch (URISyntaxException malformed) { throw new IOException("Invalid source path"); }
    }
    private static SourceTransfer.Ftp ftp(Map<String,Object> route, URI uri) throws IOException, InterruptedException {
        SourceTransfer.Ftp ftp = new SourceTransfer.Ftp(route, uri);
        try {
            Map<String,Object> auth = SourceAuth.forUri(uri, route);
            ftp.login(String.valueOf(auth.getOrDefault("username", "anonymous")), String.valueOf(auth.getOrDefault("password", "MultiRoute-Downloader@")));
            ftp.expect("TYPE I", 200);
            SourceTransfer.Reply utf8 = ftp.command("OPTS UTF8 ON");
            if (uri.getPath().chars().anyMatch(c -> c > 127) && utf8.code() != 200 && utf8.code() != 202)
                throw new Transport.NonRetryableException("FTP server must support UTF-8 for Unicode paths");
            return ftp;
        } catch (IOException failure) { ftp.close(); throw failure; }
    }
    private static Socket data(Map<String,Object> route, URI uri, SourceTransfer.Ftp ftp, String command) throws IOException, InterruptedException {
        int port = ftp.passivePort(); Socket socket = SourceTransfer.connect(route, uri.getHost(), port);
        try {
            SourceTransfer.Reply reply = ftp.command(command);
            if (reply.code() != 125 && reply.code() != 150) throw new IOException("FTP server refused the file transfer");
            return ftp.secure ? SourceTransfer.tls(socket, uri.getHost(), port) : socket;
        } catch (IOException failure) { try { socket.close(); } catch (IOException ignored) { } throw failure; }
    }
    private static void finishData(SourceTransfer.Ftp ftp) throws IOException {
        SourceTransfer.Reply reply = ftp.read();
        if (reply.code() != 226 && reply.code() != 250) throw new IOException("FTP server refused the file transfer");
    }
    private static long ftpSize(SourceTransfer.Ftp ftp, String path) throws IOException {
        SourceTransfer.Reply reply = ftp.command("SIZE " + path);
        if (reply.code() == 550) return -1;
        if (reply.code() != 213) throw new Transport.NonRetryableException("FTP SIZE is required for safe uploads");
        try { long size = Long.parseLong(reply.text().trim()); if (size < 0) throw new NumberFormatException(); return size; }
        catch (NumberFormatException malformed) { throw new IOException("Invalid FTP file size"); }
    }
    private static void ftpUpload(Map<String,Object> route, URI uri, Path local, String temporary, long size,
            String sha256, boolean overwrite, SourceTransfer.UploadControl control) throws IOException, InterruptedException {
        try (SourceTransfer.Ftp ftp = ftp(route, uri)) {
            control.attach(ftp);
            try {
                if (!overwrite && ftpSize(ftp, uri.getPath()) >= 0) throw new Transport.NonRetryableException("Destination file already exists");
                long offset = ftpSize(ftp, temporary);
                if (offset > size) throw new Transport.NonRetryableException("Remote partial file differs from local file");
                if (offset > 0) {
                    control.stage("verifying");
                    try (Socket remote = data(route, uri, ftp, "RETR " + temporary)) {
                        control.attach(remote);
                        try { verifyPrefix(remote.getInputStream(), local, offset, control); }
                        finally { control.detach(remote); }
                    }
                    finishData(ftp);
                }
                if (offset < 0) offset = 0;
                if (offset < size || size == 0) {
                    control.stage("running");
                    int port = ftp.passivePort();
                    try (Socket raw = SourceTransfer.connect(route, uri.getHost(), port)) {
                        control.attach(raw);
                        try {
                            if (offset > 0 && ftp.command("REST " + offset).code() != 350) offset = 0;
                            control.progress(offset);
                            SourceTransfer.Reply stor = ftp.command("STOR " + temporary);
                            if (stor.code() != 125 && stor.code() != 150) throw new IOException("FTP server refused the file transfer");
                            Socket stream = ftp.secure ? SourceTransfer.tls(raw, uri.getHost(), port) : raw;
                            control.attach(stream);
                            try { copy(local, offset, size, stream.getOutputStream(), control); }
                            finally { control.detach(stream); if (stream != raw) stream.close(); }
                        } finally { control.detach(raw); }
                    }
                    finishData(ftp);
                }
                control.check(); control.stage("verifying");
                if (ftpSize(ftp, temporary) != size) throw new IOException("Incorrect final size");
                try (Socket remote = data(route, uri, ftp, "RETR " + temporary)) {
                    control.attach(remote);
                    try { verifyHash(remote.getInputStream(), size, sha256, control); }
                    finally { control.detach(remote); }
                }
                finishData(ftp); control.check();
                if (!overwrite && ftpSize(ftp, uri.getPath()) >= 0) throw new Transport.NonRetryableException("Destination file already exists");
                ftp.expect("RNFR " + temporary, 350); ftp.expect("RNTO " + uri.getPath(), 250);
                if (ftpSize(ftp, uri.getPath()) != size) throw new IOException("Incorrect final size");
                control.progress(size);
            } finally { control.detach(ftp); }
        }
    }
    private static void copy(Path local, long offset, long size, OutputStream output, SourceTransfer.UploadControl control) throws IOException {
        try (InputStream input = Files.newInputStream(local, LinkOption.NOFOLLOW_LINKS)) {
            input.skipNBytes(offset); long sent = offset; byte[] buffer = new byte[65536];
            while (sent < size) {
                control.check(); int read = input.read(buffer, 0, (int)Math.min(buffer.length, size - sent));
                if (read < 0) throw new IOException("Local file changed during upload");
                if (read == 0) continue;
                output.write(buffer, 0, read); sent += read; control.progress(sent);
            }
            if (input.read() != -1) throw new IOException("Local file changed during upload");
            output.flush();
        }
    }
    private static void verifyPrefix(InputStream remote, Path local, long length, SourceTransfer.UploadControl control) throws IOException {
        try (InputStream input = Files.newInputStream(local, LinkOption.NOFOLLOW_LINKS)) {
            long remaining = length; byte[] remoteBytes = new byte[65536], localBytes = new byte[65536];
            while (remaining > 0) {
                control.check(); int read = remote.read(remoteBytes, 0, (int)Math.min(remoteBytes.length, remaining));
                if (read < 0 || input.readNBytes(localBytes, 0, Math.max(0, read)) != read ||
                        !Arrays.equals(remoteBytes, 0, Math.max(0, read), localBytes, 0, Math.max(0, read)))
                    throw new Transport.NonRetryableException("Remote partial file differs from local file");
                remaining -= read;
            }
            if (remote.read() != -1) throw new IOException("Remote partial file is still changing; retry upload");
        }
    }
    private static void verifyHash(InputStream remote, long size, String expected, SourceTransfer.UploadControl control) throws IOException {
        MessageDigest digest = Main.sha(); byte[] buffer = new byte[65536]; long received = 0; int read;
        while ((read = remote.read(buffer)) >= 0) {
            control.check(); if (read == 0) continue; received += read;
            if (received > size) throw new IOException("Incorrect final size");
            digest.update(buffer, 0, read);
        }
        if (received != size || !Main.hex(digest.digest()).equals(expected)) throw new Transport.NonRetryableException("Uploaded file checksum does not match local SHA-256");
    }
    private static List<Map<String,Object>> ftpList(Map<String,Object> route, URI uri) throws IOException, InterruptedException {
        try (SourceTransfer.Ftp ftp = ftp(route, uri)) {
            String path = uri.getPath().isEmpty() ? "/" : uri.getPath(); String content;
            boolean machine = true;
            try { content = listingData(route, uri, ftp, "MLSD " + path); }
            catch (IOException unsupported) { machine = false; content = listingData(route, uri, ftp, "LIST " + path); }
            List<Map<String,Object>> entries = new ArrayList<>();
            for (String line : content.split("\\r?\\n")) {
                if (line.isEmpty() || line.startsWith("total ")) continue;
                String name; boolean directory; long size; String modified = "";
                if (machine) {
                    int split = line.indexOf(' '); if (split < 1) throw new IOException("Invalid remote directory listing");
                    Map<String,String> facts = new HashMap<>();
                    for (String fact : line.substring(0, split).split(";")) { int eq = fact.indexOf('='); if (eq > 0) facts.put(fact.substring(0,eq).toLowerCase(Locale.ROOT), fact.substring(eq+1)); }
                    String type = facts.getOrDefault("type", "").toLowerCase(Locale.ROOT);
                    if (type.equals("cdir") || type.equals("pdir") || type.contains("slink")) continue;
                    if (!(type.equals("dir") || type.equals("file"))) continue;
                    name = line.substring(split+1); directory = type.equals("dir");
                    try { size = directory ? 0 : Long.parseLong(facts.getOrDefault("size", "-1")); }
                    catch (NumberFormatException malformed) { throw new IOException("Invalid file size"); }
                    String stamp = facts.getOrDefault("modify", "");
                    if (stamp.matches("[0-9]{14}(?:\\.[0-9]+)?")) try { modified = LocalDateTime.parse(stamp.substring(0,14), DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).toInstant(ZoneOffset.UTC).toString(); } catch (RuntimeException ignored) { }
                } else {
                    String[] fields = line.split("\\s+", 9);
                    if (fields.length != 9 || !(fields[0].startsWith("-") || fields[0].startsWith("d"))) throw new IOException("FTP server requires MLSD or Unix LIST for directory browsing");
                    name = fields[8]; directory = fields[0].startsWith("d");
                    try { size = directory ? 0 : Long.parseLong(fields[4]); } catch (NumberFormatException malformed) { throw new IOException("Invalid file size"); }
                    modified = fields[5] + " " + fields[6] + " " + fields[7];
                }
                if (!nameSafe(name)) continue;
                if (entries.size() >= 10000) throw new IOException("Folder contains more than 10,000 entries. Select an individual subfolder.");
                entries.add(entry(uri, name, directory, size, modified));
            }
            return entries;
        }
    }
    private static String listingData(Map<String,Object> route, URI uri, SourceTransfer.Ftp ftp, String command) throws IOException, InterruptedException {
        byte[] bytes;
        try (Socket socket = data(route, uri, ftp, command)) { bytes = socket.getInputStream().readNBytes(4 * 1024 * 1024 + 1); }
        if (bytes.length > 4 * 1024 * 1024) throw new IOException("Remote directory listing exceeds 4 MiB");
        finishData(ftp);
        try { return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString(); }
        catch (java.nio.charset.CharacterCodingException malformed) { throw new IOException("Invalid UTF-8 directory listing"); }
    }
    private static final class Sftp implements AutoCloseable {
        final Session session; final ChannelSftp channel;
        Sftp(Map<String,Object> route, URI uri) throws IOException, InterruptedException {
            Map<String,Object> auth = SourceAuth.forUri(uri, route); String fingerprint = Main.str(auth.get("host_key_sha256"));
            if (fingerprint.isEmpty() || !(auth.get("username") instanceof String username)) throw new Transport.NonRetryableException("Configure SFTP credentials and the verified server host-key fingerprint first");
            Session started = null; ChannelSftp opened = null;
            try {
                JSch client = new JSch(); client.setHostKeyRepository(new SourceTransfer.PinnedKey(fingerprint));
                started = client.getSession(username, uri.getHost(), uri.getPort() < 0 ? 22 : uri.getPort());
                started.setConfig("StrictHostKeyChecking", "yes"); started.setConfig("PreferredAuthentications", "password,keyboard-interactive");
                started.setPassword(Main.str(auth.get("password")).getBytes(StandardCharsets.UTF_8)); started.setTimeout(60000);
                started.setSocketFactory(new com.jcraft.jsch.SocketFactory() {
                    public Socket createSocket(String host, int port) throws IOException { try { return SourceTransfer.connect(route,host,port); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("Source connection interrupted"); } }
                    public InputStream getInputStream(Socket socket) throws IOException { return socket.getInputStream(); }
                    public OutputStream getOutputStream(Socket socket) throws IOException { return socket.getOutputStream(); }
                });
                started.connect(15000); opened = (ChannelSftp)started.openChannel("sftp"); opened.connect(15000); opened.setFilenameEncoding(StandardCharsets.UTF_8);
                session = started; channel = opened;
            } catch (JSchException failure) { if (opened != null) opened.disconnect(); if (started != null) started.disconnect(); throw new Transport.NonRetryableException("SFTP connection, host-key verification or authentication failed", failure); }
        }
        public void close() { channel.disconnect(); session.disconnect(); }
        SftpATTRS stat(String path) throws IOException {
            try { return channel.lstat(literal(path)); }
            catch (SftpException failure) { if (failure.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) return null; throw new IOException("Could not inspect remote file", failure); }
        }
    }
    private static void sftpUpload(Map<String,Object> route, URI uri, Path local, String temporary, long size,
            String sha256, boolean overwrite, SourceTransfer.UploadControl control) throws IOException, InterruptedException {
        try (Sftp sftp = new Sftp(route,uri)) {
            control.attach(sftp);
            try {
                if (!overwrite && sftp.stat(uri.getPath()) != null) throw new Transport.NonRetryableException("Destination file already exists");
                SftpATTRS attrs = sftp.stat(temporary); long offset = attrs == null ? 0 : attrs.getSize();
                if (attrs != null && (!attrs.isReg() || attrs.isLink()) || offset > size) throw new Transport.NonRetryableException("Remote partial file differs from local file");
                if (offset > 0) { control.stage("verifying"); try (InputStream input = sftp.channel.get(literal(temporary))) { control.attach(input); try { verifyPrefix(input,local,offset,control); } finally { control.detach(input); } } }
                control.check();
                if (offset < size || size == 0) {
                    control.stage("running"); control.progress(offset);
                    try (OutputStream output = sftp.channel.put(literal(temporary),(SftpProgressMonitor)null,offset > 0 ? ChannelSftp.APPEND : ChannelSftp.OVERWRITE)) {
                        control.attach(output); try { copy(local,offset,size,output,control); } finally { control.detach(output); }
                    }
                }
                control.check(); control.stage("verifying"); attrs = sftp.stat(temporary);
                if (attrs == null || !attrs.isReg() || attrs.getSize() != size) throw new IOException("Incorrect final size");
                try (InputStream input = sftp.channel.get(literal(temporary))) { control.attach(input); try { verifyHash(input,size,sha256,control); } finally { control.detach(input); } }
                control.check(); if (!overwrite && sftp.stat(uri.getPath()) != null) throw new Transport.NonRetryableException("Destination file already exists");
                sftp.channel.rename(literal(temporary),literal(uri.getPath())); attrs = sftp.stat(uri.getPath());
                if (attrs == null || attrs.getSize() != size) throw new IOException("Incorrect final size");
                control.progress(size);
            } catch (SftpException failure) { throw new IOException("SFTP upload or remote verification failed",failure); }
            finally { control.detach(sftp); }
        }
    }
    private static List<Map<String,Object>> sftpList(Map<String,Object> route, URI uri) throws IOException, InterruptedException {
        try (Sftp sftp = new Sftp(route,uri)) {
            List<Map<String,Object>> result = new ArrayList<>();
            try {
                sftp.channel.ls(literal(uri.getPath().isEmpty() ? "/" : uri.getPath()), item -> {
                    String name = item.getFilename(); SftpATTRS attrs = item.getAttrs();
                    if (!nameSafe(name) || attrs.isLink() || !(attrs.isReg() || attrs.isDir())) return ChannelSftp.LsEntrySelector.CONTINUE;
                    if (result.size() >= 10000) return ChannelSftp.LsEntrySelector.BREAK;
                    try { result.add(entry(uri,name,attrs.isDir(),attrs.getSize(),Instant.ofEpochSecond(Integer.toUnsignedLong(attrs.getMTime())).toString())); }
                    catch (IOException invalid) { throw new IllegalArgumentException(invalid); }
                    return ChannelSftp.LsEntrySelector.CONTINUE;
                });
                if (result.size() >= 10000) throw new IOException("Folder contains more than 10,000 entries. Select an individual subfolder.");
                return result;
            } catch (SftpException | IllegalArgumentException failure) { throw new IOException("Could not read remote directory",failure); }
        }
    }
}
