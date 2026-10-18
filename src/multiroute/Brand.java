package multiroute;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Display identity and narrowly scoped compatibility with existing saved installations. */
final class Brand {
    static final String NAME = "MultiRoute Downloader";
    static final String JAR = "MultiRoute-Downloader.jar";
    static final String REPOSITORY = "https://github.com/2018-spec-fdsds/multiroute-downloader";
    private Brand() { }

    /** Apply to registered templates before formatting, never to a user filename, URL or path argument. */
    static String display(String template) { return template.replace("MultiRoute Resume", NAME); }

    static boolean applicationJar(String name) {
        return JAR.equalsIgnoreCase(name) || "MultiRoute-Resume.jar".equalsIgnoreCase(name);
    }

    static Path savedProfile(Path current, Path legacy) {
        // Reuse an existing profile in place. No migration moves files or replaces settings.
        boolean previous=Files.isDirectory(legacy.resolve("data"),LinkOption.NOFOLLOW_LINKS)
                || Files.isRegularFile(legacy.resolve("data-location.config"),LinkOption.NOFOLLOW_LINKS);
        boolean present=Files.exists(current.resolve("data"),LinkOption.NOFOLLOW_LINKS)
                || Files.exists(current.resolve("data-location.config"),LinkOption.NOFOLLOW_LINKS);
        return previous&&!present?legacy:current;
    }
}
