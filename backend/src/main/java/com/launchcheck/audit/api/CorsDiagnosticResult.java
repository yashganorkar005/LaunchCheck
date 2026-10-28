package com.launchcheck.audit.api;

public record CorsDiagnosticResult(
    String endpointPath,
    String method,
    String origin,
    String status,
    String diagnostic,
    boolean preflightAttempted,
    CorsResponseHeaders responseHeaders
) {

    public record CorsResponseHeaders(
        String allowOrigin,
        String allowMethods,
        String allowHeaders,
        String allowCredentials,
        String vary
    ) {}
}
