package com.avionics_systems.cluster.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantFilter extends OncePerRequestFilter {

    @Value("${cluster.tenant.header-name:X-Tenant-ID}")
    private String tenantHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String tenantId = request.getHeader(tenantHeader);
        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setTenantId(tenantId);
            MDC.put("tenantId", tenantId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
        }
    }
}
