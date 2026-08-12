package com.vaultdesk.backend.common.exception;

import com.vaultdesk.backend.common.logging.CorrelationIdFilter;
import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        List<String> details) {

    public ErrorResponse(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, CorrelationIdFilter.currentTraceId(), List.of());
    }

    public ErrorResponse(int status, String error, String message, String path, List<String> details) {
        this(Instant.now(), status, error, message, path, CorrelationIdFilter.currentTraceId(), details);
    }
}
