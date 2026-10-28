package com.launchcheck.audit.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PinnedTlsTransportTest {
    private static final String HOST = "fixture.launchcheck.test";
    private static final String WRONG_HOST = "wrong.host.test";
    private static final char[] STORE_PASS = "changeit".toCharArray();

    private static Path serverKeystorePath;
    private static Path serverCertPath;
    private static Path truststorePath;

    private static final AtomicReference<InetAddress> peer = new AtomicReference<>();

    @BeforeAll
    static void generateLocalCertificate() throws Exception {
        Path tmpDir = Files.createTempDirectory("launchcheck_tls_");
        serverKeystorePath = tmpDir.resolve("server.p12");
        serverCertPath = tmpDir.resolve("server-cert.pem");
        truststorePath = tmpDir.resolve("truststore.p12");

        String javaHome = System.getProperty("java.home");
        Path binDir = Path.of(javaHome, "bin");
        Path keytool = binDir.resolve("keytool");
        if (!Files.exists(keytool)) {
            keytool = binDir.resolve("keytool.exe");
        }
        if (!Files.exists(keytool)) {
            // Fallback for some JRE layouts
            keytool = Path.of(javaHome, "../bin/keytool");
            if (!Files.exists(keytool)) {
                keytool = Path.of(javaHome, "../bin/keytool.exe");
            }
        }

        String alias = "server";

        runKeytool(
                keytool,
                "-genkeypair",
                "-alias",
                alias,
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-validity",
                "365",
                "-storetype",
                "PKCS12",
                "-keystore",
                serverKeystorePath.toString(),
                "-storepass",
                new String(STORE_PASS),
                "-keypass",
                new String(STORE_PASS),
                "-dname",
                "CN=" + HOST,
                "-ext",
                "SAN=dns:" + HOST,
                "-noprompt"
        );

        runKeytool(
                keytool,
                "-exportcert",
                "-alias",
                alias,
                "-keystore",
                serverKeystorePath.toString(),
                "-storepass",
                new String(STORE_PASS),
                "-rfc",
                "-file",
                serverCertPath.toString()
        );

        runKeytool(
                keytool,
                "-importcert",
                "-alias",
                alias,
                "-file",
                serverCertPath.toString(),
                "-storetype",
                "PKCS12",
                "-keystore",
                truststorePath.toString(),
                "-storepass",
                new String(STORE_PASS),
                "-noprompt"
        );
    }

    @AfterEach
    void restoreTrustStore() {
        String oldTrustStore = System.getProperty("javax.net.ssl.trustStore");
        String oldTrustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");

        // If tests set these properties, restore best-effort by clearing them.
        try {
            System.clearProperty("javax.net.ssl.trustStore");
            System.clearProperty("javax.net.ssl.trustStorePassword");
        } finally {
            if (oldTrustStore != null) System.setProperty("javax.net.ssl.trustStore", oldTrustStore);
            if (oldTrustStorePassword != null) System.setProperty("javax.net.ssl.trustStorePassword", oldTrustStorePassword);
        }
    }

    @Test
    void tlsHostnameVerificationPassesWhenHostMatchesCertificate() throws Exception {
        peer.set(null);

        setTestTrustStore();

        HttpsServer server = createServerForHost(HOST);
        try {
            int port = server.getAddress().getPort();

            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            TargetUrl target = new TargetUrl("https", HOST.toLowerCase(), port, "/");

            HttpProbeResult result = new PinnedHttpProbe(policy).check(target);

            assertEquals(204, result.httpStatus());
            assertEquals("PASS", result.status());
            assertEquals("PASS", result.tls().status());
            assertNotNull(result.tls().certificateExpiresAt());
            assertTrue(result.tls().diagnostic().toLowerCase().contains("successful"));
            assertEquals(InetAddress.getByName("127.0.0.1"), peer.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void tlsHostnameVerificationFailsWhenHostDoesNotMatchCertificate() throws Exception {
        peer.set(null);

        setTestTrustStore();

        HttpsServer server = createServerForHost(HOST);
        try {
            int port = server.getAddress().getPort();

            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, host -> List.of(InetAddress.getByName("127.0.0.1")));
            TargetUrl target = new TargetUrl("https", WRONG_HOST.toLowerCase(), port, "/");

            HttpProbeResult result = new PinnedHttpProbe(policy).check(target);

            assertEquals("FAIL", result.status());
            assertEquals(0, result.httpStatus());
            assertEquals("FAIL", result.tls().status());
            assertNull(result.tls().certificateExpiresAt());
            assertTrue(result.tls().diagnostic().toLowerCase().contains("handshake failed"));
            assertNull(peer.get());
        } finally {
            server.stop(0);
        }
    }

    private static void setTestTrustStore() {
        System.setProperty("javax.net.ssl.trustStore", truststorePath.toString());
        System.setProperty("javax.net.ssl.trustStorePassword", new String(STORE_PASS));
    }

    private static HttpsServer createServerForHost(String certificateHost) throws Exception {
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0);
        HttpsServer server = HttpsServer.create(addr, 0);

        SSLContext sslContext = buildServerSslContext();
        server.setHttpsConfigurator(
                new HttpsConfigurator(sslContext) {
                    @Override
                    public void configure(HttpsParameters params) {
                        super.configure(params);
                        // No need to customize protocols; stick to defaults.
                    }
                }
        );

        server.createContext("/", exchange -> {
            peer.set(exchange.getRemoteAddress().getAddress());
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(204, -1);
            try {
                // Ensure we don't leak content; just close.
                OutputStream os = exchange.getResponseBody();
                os.close();
            } catch (IOException ignored) {
            }
        });

        // Avoid hanging tests if something goes wrong.
        server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor());
        server.start();

        return server;
    }

    private static SSLContext buildServerSslContext() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (var in = Files.newInputStream(serverKeystorePath)) {
            ks.load(in, STORE_PASS);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, STORE_PASS);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    private static void runKeytool(Path keytool, String... args) throws Exception {
        if (!Files.exists(keytool)) {
            throw new IllegalStateException("keytool not found at: " + keytool);
        }

        List<String> cmd = new java.util.ArrayList<>();
        cmd.add(keytool.toString());
        cmd.addAll(List.of(args));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        byte[] output = p.getInputStream().readAllBytes();
        boolean finished = p.waitFor(Duration.ofSeconds(30).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new AssertionError("keytool timed out");
        }

        if (p.exitValue() != 0) {
            throw new AssertionError("keytool failed (exit=" + p.exitValue() + ") output:\n" + new String(output));
        }
    }
}
