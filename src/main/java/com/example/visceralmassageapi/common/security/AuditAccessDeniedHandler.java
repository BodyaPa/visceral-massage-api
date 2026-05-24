package com.example.visceralmassageapi.common.security;

import com.example.visceralmassageapi.common.audit.AuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuditAccessDeniedHandler implements AccessDeniedHandler {

    private final AuditLogger auditLogger;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String requestUri = request.getRequestURI();
        if (requestUri.equals("/api/admin") || requestUri.startsWith("/api/admin/")) {
            auditLogger.adminAccessDenied(request.getMethod());
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
