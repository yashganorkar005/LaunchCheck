package com.launchcheck.audit.http;

import java.util.List;
import java.util.Map;

public record HttpExchangeResult(
        String status,
        long elapsedMs,
        int httpStatus,
        String diagnostic,
        Map<String, List<String>> headers,
        String body,
        HttpProbeResult.TlsDetails tls
) {
}
