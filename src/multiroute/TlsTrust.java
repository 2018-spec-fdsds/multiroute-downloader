package multiroute;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Locale;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/** Read-only system trust integration; chain and hostname verification remain enabled. */
public final class TlsTrust {
    private static final SSLContext CONTEXT = create();

    private TlsTrust() { }

    public static SSLContext context() { return CONTEXT; }

    private static SSLContext create() {
        try {
            SSLContext defaults = SSLContext.getDefault();
            // An explicitly configured JVM trust store is an intentional trust boundary.
            if (System.getProperty("javax.net.ssl.trustStore") != null ||
                    System.getProperty("javax.net.ssl.trustStoreType") != null ||
                    System.getProperty("javax.net.ssl.trustStoreProvider") != null ||
                    System.getProperty("javax.net.ssl.keyStore") != null ||
                    System.getProperty("javax.net.ssl.keyStoreType") != null ||
                    !System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"))
                return defaults;
            try {
                TrustManagerFactory original = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                original.init((KeyStore) null);
                KeyStore trusted = KeyStore.getInstance("PKCS12");
                trusted.load(null, null);
                int index = 0;
                for (TrustManager manager : original.getTrustManagers()) {
                    if (manager instanceof X509TrustManager certificates)
                        for (Certificate certificate : certificates.getAcceptedIssuers())
                            trusted.setCertificateEntry("java-" + index++, certificate);
                }
                KeyStore windows = KeyStore.getInstance("Windows-ROOT");
                windows.load(null, null);
                for (Enumeration<String> aliases = windows.aliases(); aliases.hasMoreElements(); ) {
                    Certificate certificate = windows.getCertificate(aliases.nextElement());
                    if (certificate != null) trusted.setCertificateEntry("windows-" + index++, certificate);
                }
                TrustManagerFactory combined = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                combined.init(trusted);
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, combined.getTrustManagers(), null);
                return context;
            } catch (GeneralSecurityException | java.io.IOException unavailableWindowsStore) {
                // The Java trust store continues to validate TLS when a runtime lacks SunMSCAPI.
                return defaults;
            }
        } catch (GeneralSecurityException unavailableDefaultContext) {
            throw new ExceptionInInitializerError(unavailableDefaultContext);
        }
    }
}
