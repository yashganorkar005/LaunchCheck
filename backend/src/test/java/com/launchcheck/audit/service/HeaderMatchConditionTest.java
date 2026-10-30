package com.launchcheck.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchcheck.audit.api.ApiSmokeTestResult;
import com.launchcheck.audit.api.AuditRequest.ApiCheck;
import com.launchcheck.audit.api.AuditRequest.HeaderAssertion;
import com.launchcheck.audit.api.AuditRequest.HeaderMatchCondition;
import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.safety.UrlSafetyPolicy;
import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeaderMatchConditionTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void containsPassesSubstringAndFailsNonMatchingValue() throws Exception {
        assertEquals("PASS", runAssertion("Content-Type", List.of("application/json; charset=utf-8"),
                new HeaderAssertion("content-type", "application/json", HeaderMatchCondition.CONTAINS)).status());
        assertEquals("FAIL", runAssertion("Content-Type", List.of("application/json; charset=utf-8"),
                new HeaderAssertion("content-type", "text/html", HeaderMatchCondition.CONTAINS)).status());
    }

    @Test
    void equalsRequiresTheCompleteValue() throws Exception {
        assertEquals("PASS", runAssertion("Content-Type", List.of("application/json"),
                new HeaderAssertion("content-type", "application/json", HeaderMatchCondition.EQUALS)).status());
        assertEquals("FAIL", runAssertion("Content-Type", List.of("application/json; charset=utf-8"),
                new HeaderAssertion("content-type", "application/json", HeaderMatchCondition.EQUALS)).status());
    }

    @Test
    void startsWithPassesPrefixAndFailsWhenPrefixIsDifferent() throws Exception {
        assertEquals("PASS", runAssertion("Content-Type", List.of("application/json; charset=utf-8"),
                new HeaderAssertion("content-type", "application/json", HeaderMatchCondition.STARTS_WITH)).status());
        assertEquals("FAIL", runAssertion("Content-Type", List.of("text/application/json"),
                new HeaderAssertion("content-type", "application/json", HeaderMatchCondition.STARTS_WITH)).status());
    }

    @Test
    void endsWithPassesSuffixAndFailsWhenValueHasAdditionalSuffix() throws Exception {
        assertEquals("PASS", runAssertion("Content-Type", List.of("application/json"),
                new HeaderAssertion("content-type", "json", HeaderMatchCondition.ENDS_WITH)).status());
        assertEquals("FAIL", runAssertion("Content-Type", List.of("application/json; charset=utf-8"),
                new HeaderAssertion("content-type", "application/json", HeaderMatchCondition.ENDS_WITH)).status());
    }

    @Test
    void headerNamesAreCaseInsensitiveButHeaderValuesAreCaseSensitive() throws Exception {
        assertEquals("PASS", runAssertion("Content-Type", List.of("application/json"),
                new HeaderAssertion("cOnTeNt-TyPe", "application/json", HeaderMatchCondition.EQUALS)).status());
        assertEquals("FAIL", runAssertion("Content-Type", List.of("Application/JSON"),
                new HeaderAssertion("content-type", "application/json", HeaderMatchCondition.EQUALS)).status());
    }

    @Test
    void missingHeadersFailAndEmptyOrNullExpectedValuesOnlyAssertPresence() throws Exception {
        assertEquals("PASS", runAssertion("X-Present", List.of("value"),
                new HeaderAssertion("x-present", "", HeaderMatchCondition.EQUALS)).status());
        assertEquals("PASS", runAssertion("X-Present", List.of("value"),
                new HeaderAssertion("x-present", null, HeaderMatchCondition.EQUALS)).status());
        assertEquals("FAIL", runAssertion("X-Other", List.of("value"),
                new HeaderAssertion("x-missing", "", HeaderMatchCondition.EQUALS)).status());
        assertEquals("FAIL", runAssertion("X-Other", List.of("value"),
                new HeaderAssertion("x-missing", null, HeaderMatchCondition.EQUALS)).status());
    }

    @Test
    void missingConditionRetainsContainsBehavior() throws Exception {
        HeaderAssertion legacyAssertion = OBJECT_MAPPER.readValue(
                "{\"name\":\"content-type\",\"value\":\"application/json\"}", HeaderAssertion.class);

        assertNull(legacyAssertion.condition());
        assertEquals("PASS", runAssertion("Content-Type", List.of("application/json; charset=utf-8"), legacyAssertion).status());
    }

    @Test
    void multipleHeaderValuesUseTheFirstValueDeterministically() throws Exception {
        assertEquals("PASS", runAssertion("X-Values", List.of("first", "second"),
                new HeaderAssertion("x-values", "first", HeaderMatchCondition.EQUALS)).status());
        assertEquals("FAIL", runAssertion("X-Values", List.of("first", "second"),
                new HeaderAssertion("x-values", "second", HeaderMatchCondition.EQUALS)).status());
    }

    @Test
    void allConditionsSerializeAndDeserialize() throws Exception {
        for (HeaderMatchCondition condition : HeaderMatchCondition.values()) {
            String serialized = OBJECT_MAPPER.writeValueAsString(condition);
            assertEquals(condition, OBJECT_MAPPER.readValue(serialized, HeaderMatchCondition.class));

            String lowerCaseJson = "\"" + condition.name().toLowerCase(java.util.Locale.ROOT) + "\"";
            assertEquals(condition, OBJECT_MAPPER.readValue(lowerCaseJson, HeaderMatchCondition.class));
        }
    }

    @Test
    void invalidConditionValuesAreRejected() {
        assertThrows(JsonProcessingException.class, () -> OBJECT_MAPPER.readValue(
                "{\"name\":\"content-type\",\"value\":\"application/json\",\"condition\":\"unknown\"}",
                HeaderAssertion.class));
    }

    private ApiSmokeTestResult runAssertion(String responseHeaderName, List<String> responseHeaderValues,
                                            HeaderAssertion assertion) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/headers", exchange -> {
            for (String value : responseHeaderValues) {
                exchange.getResponseHeaders().add(responseHeaderName, value);
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            UrlSafetyPolicy policy = new UrlSafetyPolicy(addr -> true,
                    host -> List.of(InetAddress.getByName("127.0.0.1")));
            ApiSmokeTestService service = new ApiSmokeTestService(policy);
            ApiCheck check = new ApiCheck("/headers", "GET", 200, List.of(assertion), List.of());
            return service.runTests(new TargetUrl("http", "headers.example.test", port, "/"), List.of(check)).get(0);
        } finally {
            server.stop(0);
        }
    }
}
