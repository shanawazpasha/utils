package com.yourcompany.yourapp.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.UUID;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger accessLogger = LoggerFactory.getLogger("ACCESS_LOG");
    private static final String REQUEST_START_TIME = "REQUEST_START_TIME";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        request.setAttribute(REQUEST_START_TIME, startTime);
        
        // Generate request ID if not present
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        
        // Set HTTP request information in MDC
        MDC.put("requestId", requestId);
        MDC.put("httpMethod", request.getMethod());
        MDC.put("requestUri", request.getRequestURI());
        MDC.put("requestUrl", request.getRequestURL().toString());
        MDC.put("protocol", request.getProtocol());
        MDC.put("queryParams", request.getQueryString());
        MDC.put("clientIp", getClientIpAddress(request));
        MDC.put("userAgent", request.getHeader("User-Agent"));
        MDC.put("referer", request.getHeader("Referer"));
        MDC.put("accept", request.getHeader("Accept"));
        MDC.put("acceptEncoding", request.getHeader("Accept-Encoding"));
        MDC.put("acceptLanguage", request.getHeader("Accept-Language"));
        MDC.put("contentType", request.getContentType());
        MDC.put("contentLength", String.valueOf(request.getContentLength()));
        MDC.put("hasAuthorization", request.getHeader("Authorization") != null ? "true" : "false");
        
        // X-Forwarded headers
        MDC.put("xForwardedFor", request.getHeader("X-Forwarded-For"));
        MDC.put("xForwardedProto", request.getHeader("X-Forwarded-Proto"));
        MDC.put("xRealIp", request.getHeader("X-Real-IP"));
        
        // Session information
        HttpSession session = request.getSession(false);
        if (session != null) {
            MDC.put("sessionId", session.getId());
        }
        
        // User information (extract from JWT token, session, etc.)
        extractUserInfo(request);
        
        // Business context
        extractBusinessContext(request);
        
        // Set correlation ID
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        try {
            long startTime = (Long) request.getAttribute(REQUEST_START_TIME);
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            
            // Set response information in MDC
            MDC.put("responseStatus", String.valueOf(response.getStatus()));
            MDC.put("responseTime", String.valueOf(responseTime));
            MDC.put("responseStatusText", getStatusText(response.getStatus()));
            MDC.put("responseContentType", response.getContentType());
            
            // Get response size if available
            if (response instanceof ContentCachingResponseWrapper) {
                ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
                MDC.put("responseSize", String.valueOf(wrapper.getContentSize()));
            }
            
            // Log access information
            String logMessage = String.format("%s %s %s %d %dms",
                request.getMethod(),
                request.getRequestURI(),
                request.getProtocol(),
                response.getStatus(),
                responseTime);
            
            accessLogger.info(logMessage);
            
        } finally {
            // Clean up MDC
            clearMDC();
        }
    }
    
    private void extractUserInfo(HttpServletRequest request) {
        // Extract from JWT token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Parse JWT token and extract user info
            // This is pseudo-code - implement based on your JWT library
            try {
                // JwtToken token = jwtService.parseToken(authHeader.substring(7));
                // MDC.put("userId", token.getUserId());
                // MDC.put("username", token.getUsername());
                // MDC.put("userEmail", token.getEmail());
                // MDC.put("userRole", token.getRole());
                // MDC.put("userGroup", token.getGroup());
                // MDC.put("tenantId", token.getTenantId());
                // MDC.put("organizationId", token.getOrganizationId());
            } catch (Exception e) {
                // Handle token parsing error
            }
        }
        
        // Extract from session
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object userId = session.getAttribute("userId");
            if (userId != null) {
                MDC.put("userId", userId.toString());
            }
            
            Object username = session.getAttribute("username");
            if (username != null) {
                MDC.put("username", username.toString());
            }
        }
        
        // Extract from request parameters or headers
        String userId = request.getParameter("userId");
        if (userId != null) {
            MDC.put("userId", userId);
        }
    }
    
    private void extractBusinessContext(HttpServletRequest request) {
        // Extract business context from headers or parameters
        String apiVersion = request.getHeader("API-Version");
        if (apiVersion != null) {
            MDC.put("apiVersion", apiVersion);
        }
        
        String clientVersion = request.getHeader("Client-Version");
        if (clientVersion != null) {
            MDC.put("clientVersion", clientVersion);
        }
        
        String transactionId = request.getHeader("X-Transaction-ID");
        if (transactionId != null) {
            MDC.put("transactionId", transactionId);
        }
        
        // Extract from path variables or parameters
        String orderId = request.getParameter("orderId");
        if (orderId != null) {
            MDC.put("orderId", orderId);
        }
        
        String customerId = request.getParameter("customerId");
        if (customerId != null) {
            MDC.put("customerId", customerId);
        }
        
        String productId = request.getParameter("productId");
        if (productId != null) {
            MDC.put("productId", productId);
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private String getStatusText(int status) {
        switch (status) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            default: return "Unknown";
        }
    }
    
    private void clearMDC() {
        MDC.remove("requestId");
        MDC.remove("httpMethod");
        MDC.remove("requestUri");
        MDC.remove("requestUrl");
        MDC.remove("protocol");
        MDC.remove("queryParams");
        MDC.remove("clientIp");
        MDC.remove("userAgent");
        MDC.remove("referer");
        MDC.remove("accept");
        MDC.remove("acceptEncoding");
        MDC.remove("acceptLanguage");
        MDC.