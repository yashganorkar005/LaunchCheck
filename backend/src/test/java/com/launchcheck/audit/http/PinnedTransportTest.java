package com.launchcheck.audit.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.launchcheck.audit.safety.AddressResolver;
import com.launchcheck.audit.safety.GlobalAddressPolicy;
import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PinnedTransportTest {
    @Test
    void connectsOnlyToValidatedAddress() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<InetAddress> peer = new AtomicReference<>();
        server.createContext("/", exchange -> {
            peer.set(exchange.getRemoteAddress().getAddress());
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            AddressResolver resolver = host -> List.of(InetAddress.getByName("127.0.0.1"));
            // Use permissive policy for test fixture
            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, resolver);
            TargetUrl target = new TargetUrl("http", "fixture.launchcheck.test", port, "/");
            HttpProbeResult result = new PinnedHttpProbe(policy).check(target);

            assertEquals(204, result.httpStatus());
            assertEquals(InetAddress.getByName("127.0.0.1"), peer.get());
        } finally { server.stop(0); }
    }

    @Test
    void failsOverFromUnreachableCandidateToSuccessfulCandidate() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<InetAddress> peer = new AtomicReference<>();
        server.createContext("/", exchange -> {
            peer.set(exchange.getRemoteAddress().getAddress());
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            // First candidate is an unroutable IP (TEST-NET-1 192.0.2.1) which will timeout/fail,
            // Second candidate is 127.0.0.1 which succeeds.
            AddressResolver resolver = host -> List.of(
                    InetAddress.getByName("192.0.2.1"),
                    InetAddress.getByName("127.0.0.1")
            );
            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true, resolver);
            TargetUrl target = new TargetUrl("http", "failover.launchcheck.test", port, "/");

            HttpProbeResult result = new PinnedHttpProbe(policy).check(target);
            assertEquals(200, result.httpStatus());
            assertEquals("PASS", result.status());
            assertEquals(InetAddress.getByName("127.0.0.1"), peer.get());

            PinnedHttpRequester requester = new PinnedHttpRequester(policy);
            HttpExchangeResult exchangeResult = requester.execute(target, "GET", List.of());
            assertEquals(200, exchangeResult.httpStatus());
            assertEquals("PASS", exchangeResult.status());
        } finally {
            server.stop(0);
        }
    }
}