package com.stocksense.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksense.common.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/verify-otp",
            "/api/auth/reset-password"
    );

    public AuthInterceptor(TokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. Allow preflight CORS OPTIONS requests
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();

        // 2. Only intercept /api/** requests
        if (!uri.startsWith("/api/")) {
            return true;
        }

        // 3. Allow explicitly public auth endpoints
        for (String pub : PUBLIC_PREFIXES) {
            if (uri.equals(pub) || uri.startsWith(pub + "/")) {
                return true;
            }
        }

        // 4. Extract and verify Bearer token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Authentication required. Please log in.", uri);
            return false;
        }

        String token = authHeader.substring(7).trim();
        TokenService.TokenPayload payload = tokenService.parseAndVerifyToken(token);
        if (payload == null) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Session expired or token invalid. Please log in again.", uri);
            return false;
        }

        // 5. Check Role-based authorization
        if (requiresManagerRole(request.getMethod(), uri)) {
            if (!User.ROLE_MANAGER.equalsIgnoreCase(payload.getRole())) {
                writeErrorResponse(response, HttpStatus.FORBIDDEN, "Access denied: Insufficient privileges. MANAGER role required.", uri);
                return false;
            }
        }

        // Set authenticated user context
        UserContext.setCurrentUser(payload);
        request.setAttribute("currentUser", payload);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean requiresManagerRole(String method, String uri) {
        boolean isMutation = HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method)
                || HttpMethod.DELETE.matches(method);

        if (!isMutation) {
            return false;
        }

        // Product Catalog mutations (create, edit, delete product) require MANAGER
        if (uri.startsWith("/api/products")) {
            return true;
        }

        // Warehouse & Location facility modifications require MANAGER
        if (uri.startsWith("/api/warehouses") || uri.startsWith("/api/locations")) {
            return true;
        }

        // Creating or deleting whole purchase orders / delivery orders requires MANAGER
        // (Workers can execute status transitions/validations on existing orders, but not create/delete orders)
        if (HttpMethod.POST.matches(method) || HttpMethod.DELETE.matches(method)) {
            if (uri.equals("/api/receipt-orders") || uri.equals("/api/delivery-orders")) {
                return true;
            }
        }

        return false;
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message, String uri) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiError apiError = new ApiError(status.value(), status.getReasonPhrase(), message, uri);
        response.getWriter().write(objectMapper.writeValueAsString(apiError));
        response.getWriter().flush();
    }
}
