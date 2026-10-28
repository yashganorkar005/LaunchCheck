package com.launchcheck.audit.http;

import java.util.List;

public record HttpProbeResult(
    String status,
    long elapsedMs,
    int httpStatus,
    String diagnostic,
    TlsDetails tls,
    List<SecurityHeaderResult> securityHeaders
) {
    public HttpProbeResult(
        String status,
        long elapsedMs,
        int httpStatus,
        String diagnostic,
        TlsDetails tls
    ) {
        this(status, elapsedMs, httpStatus, diagnostic, tls, List.of());
    }
    public record TlsDetails(
        String status,
        String certificateExpiresAt,
        String diagnostic
    ) {}

    public record SecurityHeaderResult(
        String headerName,
        String status,
        String value,
        String diagnostic
    ) {}
}

