package com.launchcheck.audit.http;

import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SNIHostName;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;

public class PinnedHttpProbe {
    private static final long DEFAULT_CONNECT_TIMEOUT_MS = 3_000;
    private static final long DEFAULT_READ_TIMEOUT_MS = 3_000;
    private static final long DEFAULT_MAX_RESPONSE_BYTES = 64 * 1024;

    private final UrlSafetyPolicy policy;

    public PinnedHttpProbe(UrlSafetyPolicy policy) {
        this.policy = policy;
    }

    public HttpProbeResult check(TargetUrl target) {
        List<InetAddress> addresses = policy.resolveAndValidate(target);
        if (addresses.isEmpty()) {
            HttpProbeResult.TlsDetails tls = new HttpProbeResult.TlsDetails(
                    "SKIPPED",
                    null,
                    "No safe addresses resolved"
            );
            return new HttpProbeResult("FAIL", 0, 0, "No safe addresses resolved", tls);
        }

        long startMs = System.currentTimeMillis();
        Exception lastException = null;
        HttpProbeResult.TlsDetails lastTlsDetails = new HttpProbeResult.TlsDetails("SKIPPED", null, "Not attempted");

        // Try candidate addresses with failover among validated safe addresses
        for (InetAddress candidateAddress : addresses) {
            try {
                HttpProbeResult.TlsDetails tlsDetails = probeTlsIfNeeded(target, candidateAddress);
                lastTlsDetails = tlsDetails;

                if ("FAIL".equals(tlsDetails.status()) && "https".equals(target.scheme())) {
                    long duration = System.currentTimeMillis() - startMs;
                    return new HttpProbeResult(
                            "FAIL",
                            duration,
                            0,
                            "TLS validation failed: " + tlsDetails.diagnostic(),
                            tlsDetails
                    );
                }

                HttpProbeResult res = checkWithAddress(target, candidateAddress, tlsDetails, startMs);
                if ("PASS".equals(res.status()) || res.httpStatus() > 0) {
                    return res;
                }
                lastException = new IOException(res.diagnostic());
            } catch (Exception e) {
                lastException = e;
            }
        }

        long duration = System.currentTimeMillis() - startMs;
        String errMessage = lastException != null ? sanitizeException(lastException) : "All candidate addresses failed";
        return new HttpProbeResult(
                "FAIL",
                duration,
                0,
                "Connection failed across candidate addresses: " + errMessage,
                lastTlsDetails
        );
    }

    private HttpProbeResult checkWithAddress(TargetUrl target, InetAddress pinnedAddress, HttpProbeResult.TlsDetails tlsDetails, long startMs) {
        DnsResolver pinnedResolver = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                return new InetAddress[] { pinnedAddress };
            }

            @Override
            public String resolveCanonicalHostname(String host) throws UnknownHostException {
                return host;
            }
        };

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(DEFAULT_CONNECT_TIMEOUT_MS))
                .setResponseTimeout(Timeout.ofMilliseconds(DEFAULT_READ_TIMEOUT_MS))
                .setRedirectsEnabled(false)
                .build();

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setDnsResolver(pinnedResolver)
                                .build()
                )
                .build()) {

            HttpHost httpHost = new HttpHost(target.scheme(), target.host(), target.port());
            HttpGet get = new HttpGet(target.path());

            return client.execute(httpHost, get, response -> {
                long duration = System.currentTimeMillis() - startMs;
                int code = response.getCode();

                // Collect response headers case-insensitively
                java.util.Map<String, String> headerMap = new java.util.HashMap<>();
                for (org.apache.hc.core5.http.Header h : response.getHeaders()) {
                    headerMap.put(h.getName().toLowerCase(java.util.Locale.ROOT), h.getValue());
                }

                List<HttpProbeResult.SecurityHeaderResult> securityHeaders = new java.util.ArrayList<>();

                // 1. Strict-Transport-Security (HSTS)
                String hsts = headerMap.get("strict-transport-security");
                if ("https".equals(target.scheme())) {
                    if (hsts != null && !hsts.isBlank() && hsts.contains("max-age=")) {
                        securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("Strict-Transport-Security", "PASS", hsts, "HSTS is properly configured with max-age."));
                    } else if (hsts != null) {
                        securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("Strict-Transport-Security", "WARN", hsts, "HSTS header is present but may lack proper max-age directive."));
                    } else {
                        securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("Strict-Transport-Security", "WARN", null, "Missing Strict-Transport-Security header on HTTPS endpoint."));
                    }
                } else {
                    securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("Strict-Transport-Security", "SKIPPED", null, "HSTS not evaluated on plain HTTP endpoint."));
                }

                // 2. Content-Security-Policy (CSP)
                String csp = headerMap.get("content-security-policy");
                if (csp != null && !csp.isBlank()) {
                    if (csp.contains("default-src") || csp.contains("script-src")) {
                        securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("Content-Security-Policy", "PASS", csp, "CSP header is present and defines directive rules."));
                    } else {
                        securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("Content-Security-Policy", "WARN", csp, "CSP header is present but appears weak or minimal."));
                    }
                } else {
                    securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("Content-Security-Policy", "WARN", null, "Missing Content-Security-Policy header."));
                }

                // 3. X-Content-Type-Options
                String xcto = headerMap.get("x-content-type-options");
                if (xcto != null && xcto.trim().equalsIgnoreCase("nosniff")) {
                    securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("X-Content-Type-Options", "PASS", xcto, "X-Content-Type-Options is correctly set to nosniff."));
                } else if (xcto != null) {
                    securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("X-Content-Type-Options", "WARN", xcto, "X-Content-Type-Options is present but value is not 'nosniff'."));
                } else {
                    securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("X-Content-Type-Options", "WARN", null, "Missing X-Content-Type-Options header."));
                }

                // 4. X-Frame-Options
                String xfo = headerMap.get("x-frame-options");
                String cspHeader = csp != null ? csp.toLowerCase(java.util.Locale.ROOT) : "";
                boolean hasFrameAncestors = cspHeader.contains("frame-ancestors");
                if ((xfo != null && (xfo.trim().equalsIgnoreCase("DENY") || xfo.trim().equalsIgnoreCase("SAMEORIGIN"))) || hasFrameAncestors) {
                    securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("X-Frame-Options", "PASS", xfo != null ? xfo : "Covered by CSP frame-ancestors", "Framing restriction is properly enforced."));
                } else if (xfo != null) {
                    securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("X-Frame-Options", "WARN", xfo, "X-Frame-Options is present with unrecognized or permissive value."));
                } else {
                    securityHeaders.add(new HttpProbeResult.SecurityHeaderResult("X-Frame-Options", "WARN", null, "Missing X-Frame-Options header and no CSP frame-ancestors directive found."));
                }

                consumeEntityUpTo(response.getEntity(), DEFAULT_MAX_RESPONSE_BYTES);
                return new HttpProbeResult(
                        code >= 200 && code < 400 ? "PASS" : "WARN",
                        duration,
                        code,
                        "Responded with HTTP " + code,
                        tlsDetails,
                        securityHeaders
                );
            });
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startMs;
            return new HttpProbeResult(
                    "FAIL",
                    duration,
                    0,
                    "Connection failed: " + sanitizeException(e),
                    tlsDetails
            );
        }
    }

    private HttpProbeResult.TlsDetails probeTlsIfNeeded(TargetUrl target, InetAddress pinnedAddress) {
        if (!"https".equals(target.scheme())) {
            return new HttpProbeResult.TlsDetails(
                    "SKIPPED",
                    null,
                    "TLS not applicable for HTTP"
            );
        }

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();

            try (SSLSocket socket = (SSLSocket) socketFactory.createSocket()) {
                socket.setSoTimeout((int) DEFAULT_READ_TIMEOUT_MS);
                socket.connect(new InetSocketAddress(pinnedAddress, target.port()), (int) DEFAULT_CONNECT_TIMEOUT_MS);

                SSLParameters params = socket.getSSLParameters();
                params.setEndpointIdentificationAlgorithm("HTTPS");
                params.setServerNames(List.of(new SNIHostName(target.host())));
                socket.setSSLParameters(params);

                socket.startHandshake();

                X509Certificate cert = extractLeafCert(socket);
                String expiresAt = cert != null ? cert.getNotAfter().toInstant().toString() : null;

                String status = "PASS";
                String diagnostic = "TLS handshake successful";

                if (cert != null) {
                    Instant now = Instant.now();
                    Instant notAfter = cert.getNotAfter().toInstant();
                    long daysRemaining = ChronoUnit.DAYS.between(now, notAfter);

                    if (daysRemaining < 30) {
                        status = "WARN";
                        diagnostic = "Certificate expires soon: " + daysRemaining + " days remaining";
                    }
                    if (daysRemaining < 0) {
                        status = "FAIL";
                        diagnostic = "Certificate expired: " + Math.abs(daysRemaining) + " days ago";
                    }
                }

                return new HttpProbeResult.TlsDetails(
                        status,
                        expiresAt,
                        diagnostic
                );
            }
        } catch (java.net.SocketTimeoutException | java.net.ConnectException e) {
            return new HttpProbeResult.TlsDetails(
                    "SKIPPED",
                    null,
                    "TLS handshake not reached (connection timeout/failure): " + sanitizeException(e)
            );
        } catch (Exception e) {
            return new HttpProbeResult.TlsDetails(
                    "FAIL",
                    null,
                    "TLS handshake failed: " + sanitizeException(e)
            );
        }
    }

    private static X509Certificate extractLeafCert(SSLSocket socket) {
        try {
            Object[] peerCertificates = socket.getSession().getPeerCertificates();
            if (peerCertificates == null || peerCertificates.length == 0) return null;
            return (X509Certificate) peerCertificates[0];
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void consumeEntityUpTo(HttpEntity entity, long maxBytes) {
        if (entity == null) return;

        try (InputStream in = entity.getContent()) {
            byte[] buf = new byte[4096];
            long consumed = 0;
            while (consumed < maxBytes) {
                int read = in.read(buf, 0, (int) Math.min(buf.length, maxBytes - consumed));
                if (read < 0) break;
                consumed += read;
            }
        } catch (IOException ignored) {
        }
    }

    private static String sanitizeException(Throwable t) {
        String message = Objects.toString(t.getMessage(), t.getClass().getSimpleName());
        message = message.replaceAll("\\s+", " ").trim();
        return t.getClass().getSimpleName() + (message.isEmpty() ? "" : ": " + message);
    }
}
