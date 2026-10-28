package com.launchcheck.audit.service;

import com.launchcheck.audit.api.AuditRequest;
import com.launchcheck.audit.api.AuditResponse;
import com.launchcheck.audit.safety.TargetUrl;
import com.launchcheck.audit.api.CorsDiagnosticResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Iterator;
import java.util.Map;

public class BoundedAuditExecutionService {
    private static final int MAX_CONCURRENT_AUDITS = 4;
    private static final Semaphore concurrencySemaphore = new Semaphore(MAX_CONCURRENT_AUDITS, true);

    // Bounded rate limiter map with capacity limit and automatic eviction of expired entries
    private static final int MAX_TRACKED_IPS = 5000;
    private static final ConcurrentHashMap<String, RequestBucket> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 15;

    private final AuditExecutionService delegate;

    public BoundedAuditExecutionService(AuditExecutionService delegate) {
        this.delegate = delegate;
    }

    public AuditResponse executeBoundedAudit(String clientIp, String frontendUrl, String backendUrl, List<AuditRequest.ApiCheck> apiChecks) {
        checkRateLimit(clientIp != null ? clientIp : "anonymous");

        if (!concurrencySemaphore.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Server busy: maximum concurrent audits reached. Please try again shortly.");
        }

        try {
            return delegate.executeAudit(frontendUrl, backendUrl, apiChecks);
        } finally {
            concurrencySemaphore.release();
        }
    }

    public void checkRateLimit(String key) {
        long now = System.currentTimeMillis();

        // Evict expired entries or enforce hard capacity cap
        cleanupExpiredOrExcess(now);

        RequestBucket bucket = rateLimitMap.computeIfAbsent(key, k -> {
            if (rateLimitMap.size() >= MAX_TRACKED_IPS) {
                evictOldest();
            }
            return new RequestBucket(now, new AtomicInteger(0));
        });

        synchronized (bucket) {
            if (now - bucket.windowStart > 60000) {
                bucket.windowStart = now;
                bucket.count.set(0);
            }

            if (bucket.count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded: maximum 15 audits per minute allowed.");
            }
        }
    }

    private synchronized void cleanupExpiredOrExcess(long now) {
        Iterator<Map.Entry<String, RequestBucket>> it = rateLimitMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, RequestBucket> entry = it.next();
            if (now - entry.getValue().windowStart > 120000) { // 2 minutes expiry
                it.remove();
            }
        }
        if (rateLimitMap.size() >= MAX_TRACKED_IPS) {
            evictOldest();
        }
    }

    private synchronized void evictOldest() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<String, RequestBucket> entry : rateLimitMap.entrySet()) {
            if (entry.getValue().windowStart < oldestTime) {
                oldestTime = entry.getValue().windowStart;
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            rateLimitMap.remove(oldestKey);
        }
    }

    private static class RequestBucket {
        long windowStart;
        AtomicInteger count;

        RequestBucket(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
