package com.henlab.orderservice.filter;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class TracingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TracingFilter.class);
    public static final String CORRELATION_ID = "correlationId";
    private final Tracer tracer;

    public TracingFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String correlationId = httpRequest.getHeader(CORRELATION_ID);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        String userId = httpRequest.getHeader("X-User-Id");
        String companyId = httpRequest.getHeader("X-Company-Id");

        try {
            MDC.put(CORRELATION_ID, correlationId);
            if (userId != null) {
                MDC.put("userId", userId);
            }
            if (companyId != null) {
                MDC.put("companyId", companyId);
            }

            if (tracer.currentSpan() != null) {
                MDC.put("traceId", tracer.currentSpan().context().traceId());
                MDC.put("spanId", tracer.currentSpan().context().spanId());
            }

            log.info("Processing request with correlationId={}, userId={}, companyId={}", 
                    correlationId, userId, companyId);

            httpResponse.setHeader(CORRELATION_ID, correlationId);
            if (userId != null) {
                httpResponse.setHeader("X-User-Id", userId);
            }
            if (companyId != null) {
                httpResponse.setHeader("X-Company-Id", companyId);
            }

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}