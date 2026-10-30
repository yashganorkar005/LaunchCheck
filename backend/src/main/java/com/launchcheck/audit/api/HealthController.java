package com.launchcheck.audit.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<Map<String, String>> healthCheck(HttpServletRequest request) {
        Map<String, String> body = Map.of(
                "status", "UP",
                "service", "launchcheck-api"
        );
        if ("HEAD".equals(request.getMethod())) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(body);
    }
}
