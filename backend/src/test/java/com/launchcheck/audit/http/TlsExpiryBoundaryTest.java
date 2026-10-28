package com.launchcheck.audit.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TlsExpiryBoundaryTest {
    private static final String HOST = "expiry.launchcheck.test";
    private static final char[] STORE_PASS = "changeit".toCharArray();

    private static Path serverKeystorePath;
    private static Path truststorePath;

    @BeforeAll
    static void generateCertificate() throws Exception {
        Path tmpDir = Files.createTempDirectory("launchcheck_expiry_");
        serverKeystorePath = tmpDir.resolve("server.p12");
        truststorePath = tmpDir.resolve("truststore.p12");

        // Simple keytool invocation for local cert
        String javaHome = System.getProperty("java.home");
        Path keytool = Path.of(javaHome, "bin", "keytool");
        if (!Files.exists(keytool)) keytool = Path.of(javaHome, "bin", "keytool.exe");

        runKeytool(keytool, "-genkeypair", "-alias", "server", "-keyalg", "RSA", "-keysize", "2048",
                   "-validity", "10", // 10 days validity to trigger expiration
                   "-storetype", "PKCS12", "-keystore", serverKeystorePath.toString(),
                   "-storepass", new String(STORE_PASS), "-keypass", new String(STORE_PASS),
                   "-dname", "CN=" + HOST, "-ext", "SAN=dns:" + HOST, "-noprompt");

        runKeytool(keytool, "-exportcert", "-alias", "server", "-keystore", serverKeystorePath.toString(),
                   "-storepass", new String(STORE_PASS), "-rfc", "-file", tmpDir.resolve("server.pem").toString());

        runKeytool(keytool, "-importcert", "-alias", "server", "-file", tmpDir.resolve("server.pem").toString(),
                   "-storetype", "PKCS12", "-keystore", truststorePath.toString(),
                   "-storepass", new String(STORE_PASS), "-noprompt");
    }

    @Test
    void testCertificateApproachingExpiry() throws Exception {
        System.setProperty("javax.net.ssl.trustStore", truststorePath.toString());
        System.setProperty("javax.net.ssl.trustStorePassword", new String(STORE_PASS));

        HttpsServer server = createServer();
        try {
            int port = server.getAddress().getPort();
            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            TargetUrl target = new TargetUrl("https", HOST, port, "/");

            HttpProbeResult result = new PinnedHttpProbe(policy).check(target);

            // Since it's only 10 days valid, it should be WARN
            assertEquals("WARN", result.tls().status());
            assertTrue(result.tls().diagnostic().contains("days remaining"), "Diagnostic should mention days remaining");
        } finally {
            server.stop(0);
        }
    }

    private HttpsServer createServer() throws Exception {
        HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        SSLContext sslContext = buildSslContext();
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        server.createContext("/", ex -> {
            ex.sendResponseHeaders(204, -1);
            ex.close();
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        return server;
    }

    private SSLContext buildSslContext() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (var in = Files.newInputStream(serverKeystorePath)) { ks.load(in, STORE_PASS); }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, STORE_PASS);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, new SecureRandom());
        return ctx;
    }

    private static void runKeytool(Path keytool, String... args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(keytool.toString(), "-noprompt");
        List<String> cmd = pb.command();
        cmd.addAll(List.of(args));
        Process p = pb.start();
        if (p.waitFor() != 0) throw new AssertionError("keytool failed");
    }
}
