package com.launchcheck.audit.http;

import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;

public class PinnedHttpRequester {
    private static final long DEFAULT_CONNECT_TIMEOUT_MS = 3_000;
    private static final long DEFAULT_READ_TIMEOUT_MS = 3_000;
    private static final long DEFAULT_MAX_RESPONSE_BYTES = 64 * 1024;

    private final UrlSafetyPolicy policy;

    public PinnedHttpRequester(UrlSafetyPolicy policy) {
        this.policy = policy;
    }

    public HttpExchangeResult execute(TargetUrl target, String method, List<String> headers) {
        List<InetAddress> addresses = policy.resolveAndValidate(target);
        if (addresses.isEmpty()) {
            return new HttpExchangeResult("FAIL", 0, 0, "No safe addresses resolved", Map.of(), null, null);
        }

        long startMs = System.currentTimeMillis();
        Exception lastException = null;

        // Try candidate addresses with failover among validated safe addresses
        for (InetAddress candidateAddress : addresses) {
            try {
                HttpExchangeResult res = executeWithAddress(target, candidateAddress, method, headers, startMs);
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
        return new HttpExchangeResult("FAIL", duration, 0, "Connection failed across candidate addresses: " + errMessage, Map.of(), null, null);
    }

    private HttpExchangeResult executeWithAddress(TargetUrl target, InetAddress pinnedAddress, String method, List<String> headers, long startMs) {
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
            HttpUriRequestBase request = createRequest(method, target.path());

            if (headers != null) {
                for (int i = 0; i < headers.size(); i += 2) {
                    if (i + 1 < headers.size()) {
                        request.addHeader(headers.get(i), headers.get(i + 1));
                    }
                }
            }

            return client.execute(httpHost, request, response -> {
                long duration = System.currentTimeMillis() - startMs;
                int code = response.getCode();

                Map<String, List<String>> responseHeaders = new HashMap<>();
                for (Header h : response.getHeaders()) {
                    responseHeaders.computeIfAbsent(h.getName(), k -> new ArrayList<>()).add(h.getValue());
                }

                HttpEntity entity = response.getEntity();
                String body = null;
                if (entity != null) {
                    body = EntityUtils.toString(entity, "UTF-8");
                    consumeEntityUpTo(entity, DEFAULT_MAX_RESPONSE_BYTES);
                }

                return new HttpExchangeResult(
                        code >= 200 && code < 400 ? "PASS" : "WARN",
                        duration,
                        code,
                        "Responded with HTTP " + code,
                        responseHeaders,
                        body,
                        null
                );
            });
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startMs;
            return new HttpExchangeResult("FAIL", duration, 0, "Connection failed: " + sanitizeException(e), Map.of(), null, null);
        }
    }

    private HttpUriRequestBase createRequest(String method, String path) {
        return switch (method.toUpperCase()) {
            case "OPTIONS" -> new HttpOptions(path);
            case "HEAD" -> new HttpHead(path);
            default -> new HttpGet(path);
        };
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
