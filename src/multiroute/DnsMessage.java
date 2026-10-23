package multiroute;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.IDN;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Bounded DNS wire parsing shared by the managed resolver and its checks. */
public final class DnsMessage {
    public static final int MAX_MESSAGE = 65535;
    private static final int MAX_RECORDS = 128;
    private DnsMessage() { }

    public static byte[] queryA(String name) throws IOException {
        String host = hostname(name);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeShort(ThreadLocalRandom.current().nextInt(65536));
            out.writeShort(0x0100); out.writeShort(1);
            out.writeShort(0); out.writeShort(0); out.writeShort(0);
            writeName(out, host); out.writeShort(1); out.writeShort(1);
        }
        return bytes.toByteArray();
    }

    public static void validateResponse(byte[] query, byte[] answer) throws IOException {
        Parsed question = parse(query, false), response = parse(answer, true);
        if (question.id != response.id || !question.name.equals(response.name)
                || question.type != response.type || question.klass != response.klass)
            throw new IOException("DNS response does not match its query");
    }

    public static int rcode(byte[] answer) throws IOException { return parse(answer, true).rcode; }

    public static List<String> publicAddresses(byte[] query, byte[] answer) throws IOException {
        validateResponse(query, answer);
        Parsed parsed = parse(answer, true);
        if (parsed.rcode != 0) throw new IOException("DNS returned response code " + parsed.rcode);
        String current = parsed.name;
        Set<String> seen = new HashSet<>();
        for (int hop = 0; hop <= 8; hop++) {
            if (!seen.add(current)) throw new IOException("DNS CNAME loop");
            String alias = null;
            LinkedHashSet<String> addresses = new LinkedHashSet<>();
            for (Record record : parsed.records) {
                if (!record.answer || record.klass != 1 || !record.owner.equals(current)) continue;
                if (record.type == 1) addresses.add(ipText(record.address));
                if (record.type == 5) {
                    if (alias != null && !alias.equals(record.alias)) throw new IOException("Conflicting DNS CNAME targets");
                    alias = record.alias;
                }
            }
            if (!addresses.isEmpty()) {
                if (alias != null) throw new IOException("DNS name has both CNAME and A records");
                return List.copyOf(addresses);
            }
            if (alias == null || hop == 8) break;
            current = alias;
        }
        throw new IOException("DNS response has no usable public IPv4 answer");
    }

    static String questionName(byte[] query) throws IOException { return parse(query, false).name; }

    static byte[] systemAnswer(byte[] query, InetAddress[] results) throws IOException {
        Parsed question = parse(query, false);
        LinkedHashSet<InetAddress> addresses = new LinkedHashSet<>();
        for (InetAddress address : results) {
            if (address instanceof Inet4Address && isPublic(address.getAddress())) addresses.add(address);
            if (addresses.size() == 16) break;
        }
        if (addresses.isEmpty()) throw new IOException("System DNS has no usable public IPv4 answer");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeShort(question.id); out.writeShort(0x8080 | (u16(query, 2) & 0x0100));
            out.writeShort(1); out.writeShort(addresses.size()); out.writeShort(0); out.writeShort(0);
            writeName(out, question.name); out.writeShort(1); out.writeShort(1);
            for (InetAddress address : addresses) {
                out.writeShort(0xc00c); out.writeShort(1); out.writeShort(1);
                out.writeInt(0); // OS/JVM lookup exposes no DNS TTL: never invent cache lifetime.
                out.writeShort(4); out.write(address.getAddress());
            }
        }
        byte[] response = bytes.toByteArray();
        validateResponse(query, response);
        return response;
    }

    static byte[] reduceTtlsByAge(byte[] response, long age) throws IOException {
        if (age < 0) throw new IOException("Invalid DoH Age");
        Parsed parsed = parse(response, true);
        byte[] result = response.clone();
        for (Record record : parsed.records) {
            if (record.type == 41) continue; // OPT's TTL field contains flags, version and extended RCODE.
            long ttl = Math.max(0, record.ttl - Math.min(age, 0xffffffffL));
            int offset = record.ttlOffset;
            for (int i = 3; i >= 0; i--) { result[offset + i] = (byte)ttl; ttl >>>= 8; }
        }
        return result;
    }

    static String hostname(String value) throws IOException {
        if (value == null || value.isEmpty() || !value.equals(value.trim())) throw new IOException("Invalid DNS hostname");
        String input = value.endsWith(".") ? value.substring(0, value.length() - 1) : value;
        try {
            String ascii = IDN.toASCII(input, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
            if (ascii.isEmpty() || ascii.length() > 253) throw new IllegalArgumentException();
            for (String label : ascii.split("\\.", -1))
                if (label.isEmpty() || label.length() > 63 || !label.matches("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")) throw new IllegalArgumentException();
            return ascii;
        } catch (IllegalArgumentException bad) { throw new IOException("Invalid DNS hostname", bad); }
    }

    static byte[] ipv4(String value) throws IOException {
        if (value == null) throw new IOException("Expected a literal public IPv4 address");
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) throw new IOException("Expected a literal public IPv4 address");
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            if (!parts[i].matches("0|[1-9][0-9]{0,2}")) throw new IOException("Invalid literal IPv4 address");
            int octet = Integer.parseInt(parts[i]);
            if (octet > 255) throw new IOException("Invalid literal IPv4 address");
            result[i] = (byte)octet;
        }
        if (!isPublic(result)) throw new IOException("DNS endpoint must use a public IPv4 address");
        return result;
    }

    /** Same public-destination exclusions as the native gateway transport. */
    static boolean isPublic(byte[] address) {
        if (address == null || address.length != 4) return false;
        int a = address[0] & 255, b = address[1] & 255, c = address[2] & 255;
        return a > 0 && a < 224 && a != 10 && a != 127 && !(a == 169 && b == 254)
                && !(a == 172 && b >= 16 && b <= 31) && !(a == 192 && b == 168)
                && !(a == 100 && b >= 64 && b <= 127)
                && !(a == 192 && b == 0 && (c == 0 || c == 2)) && !(a == 192 && b == 88 && c == 99)
                && !(a == 198 && (b == 18 || b == 19)) && !(a == 198 && b == 51 && c == 100)
                && !(a == 203 && b == 0 && c == 113);
    }

    private static String ipText(byte[] bytes) {
        return (bytes[0] & 255) + "." + (bytes[1] & 255) + "." + (bytes[2] & 255) + "." + (bytes[3] & 255);
    }

    private record Name(String text, int next) { }
    private record Record(String owner, int type, int klass, long ttl, int ttlOffset, byte[] address, String alias, boolean answer) { }
    private record Parsed(int id, String name, int type, int klass, int rcode, List<Record> records) { }

    private static Parsed parse(byte[] bytes, boolean response) throws IOException {
        if (bytes == null || bytes.length < 12 || bytes.length > (response ? MAX_MESSAGE : 4096)) throw new IOException("Invalid DNS message size");
        int flags = u16(bytes, 2);
        if (((flags & 0x8000) != 0) != response || (flags & 0x7800) != 0 || (flags & 0x0200) != 0 || u16(bytes, 4) != 1)
            throw new IOException("Invalid or truncated DNS header");
        int answers = u16(bytes, 6), authorities = u16(bytes, 8), additional = u16(bytes, 10);
        if (answers > 64 || answers + authorities + additional > MAX_RECORDS || (!response && (answers != 0 || authorities != 0 || (flags & 15) != 0)))
            throw new IOException("DNS record count or query flags are invalid");
        Name question = readName(bytes, 12);
        int offset = question.next;
        if (question.text.isEmpty() || offset + 4 > bytes.length || u16(bytes, offset) != 1 || u16(bytes, offset + 2) != 1)
            throw new IOException("Managed DNS supports one IN A question");
        int type = u16(bytes, offset), klass = u16(bytes, offset + 2), rcode = flags & 15;
        offset += 4;
        List<Record> records = new ArrayList<>();
        for (int i = 0; i < answers + authorities + additional; i++) {
            Name owner = readName(bytes, offset); offset = owner.next;
            if (offset + 10 > bytes.length) throw new IOException("Truncated DNS record");
            int rrType = u16(bytes, offset), rrClass = u16(bytes, offset + 2), ttlOffset = offset + 4;
            long ttl = u32(bytes, ttlOffset);
            int length = u16(bytes, offset + 8); offset += 10;
            int end = offset + length;
            if (end > bytes.length) throw new IOException("Truncated DNS record data");
            byte[] address = null; String alias = null;
            if (i < answers && rrType == 1 && rrClass == 1) {
                if (length != 4) throw new IOException("Invalid DNS A record length");
                address = Arrays.copyOfRange(bytes, offset, end);
                if (!isPublic(address)) throw new IOException("DNS returned a non-public IPv4 answer");
            } else if (i < answers && rrType == 5 && rrClass == 1) {
                Name target = readName(bytes, offset);
                if (target.next != end || target.text.isEmpty()) throw new IOException("Invalid DNS CNAME record");
                alias = target.text;
            }
            if (rrType == 41) {
                if (i < answers + authorities || !owner.text.isEmpty()) throw new IOException("Invalid DNS OPT record");
                rcode |= (int)(ttl >>> 24) << 4;
            }
            records.add(new Record(owner.text, rrType, rrClass, ttl, ttlOffset, address, alias, i < answers));
            offset = end;
        }
        if (offset != bytes.length) throw new IOException("Unexpected trailing DNS bytes");
        return new Parsed(u16(bytes, 0), question.text, type, klass, rcode, records);
    }

    private static Name readName(byte[] bytes, int start) throws IOException {
        int position = start, next = -1, jumps = 0, expanded = 0;
        StringBuilder name = new StringBuilder();
        while (true) {
            if (position >= bytes.length) throw new IOException("Truncated DNS name");
            int marker = position, length = bytes[position++] & 255;
            if (length == 0) return new Name(name.toString(), next < 0 ? position : next);
            if ((length & 0xc0) == 0xc0) {
                if (position >= bytes.length || ++jumps > 32) throw new IOException("Invalid DNS name compression");
                int pointer = ((length & 63) << 8) | (bytes[position++] & 255);
                if (pointer >= marker) throw new IOException("Forward or looping DNS compression pointer");
                if (next < 0) next = position;
                position = pointer;
                continue;
            }
            if ((length & 0xc0) != 0 || length > 63 || position + length > bytes.length || (expanded += length + 1) > 254)
                throw new IOException("Invalid DNS label length");
            if (name.length() > 0) name.append('.');
            for (int i = 0; i < length; i++) {
                int ch = bytes[position++] & 255;
                if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '-'))
                    throw new IOException("Invalid DNS name character");
                name.append((char)Character.toLowerCase(ch));
            }
        }
    }

    private static void writeName(DataOutputStream out, String name) throws IOException {
        for (String label : name.split("\\.")) {
            byte[] bytes = label.getBytes(StandardCharsets.US_ASCII);
            out.writeByte(bytes.length); out.write(bytes);
        }
        out.writeByte(0);
    }
    private static int u16(byte[] bytes, int offset) { return ((bytes[offset] & 255) << 8) | (bytes[offset + 1] & 255); }
    private static long u32(byte[] bytes, int offset) { return ((long)u16(bytes, offset) << 16) | u16(bytes, offset + 2); }
}
