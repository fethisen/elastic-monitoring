package com.elastic.monitoring.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Slf4j
public class RequestResponseLoggingAspect {

    private final ObjectMapper objectMapper;

    public RequestResponseLoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Controller package'ındaki tüm public methodları yakala
     */
    @Pointcut("execution(public * com.elastic.monitoring.controller..*.*(..))")
    public void controllerMethods() {
    }

    /**
     * Controller methodlarını intercept et ve log'la
     */
    @Around("controllerMethods()")
    public Object logRequestResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // Correlation ID oluştur - Tüm request/response loglarını birbirine bağlar
        String correlationId = java.util.UUID.randomUUID().toString();
        
        // HTTP Request bilgilerini al
        HttpServletRequest request = getCurrentHttpRequest();
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String requestUri = request != null ? request.getRequestURI() : "UNKNOWN";
        String queryString = request != null ? request.getQueryString() : null;
        String fullUri = queryString != null ? requestUri + "?" + queryString : requestUri;
        String clientIp = request != null ? getClientIp(request) : "UNKNOWN";
        String userAgent = request != null ? request.getHeader("User-Agent") : "UNKNOWN";
        String sessionId = request != null && request.getSession(false) != null 
                ? request.getSession(false).getId() : "NO_SESSION";
        
        // Method bilgilerini al
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        
        // Request payload'ı hazırla
        Map<String, Object> requestPayload = extractRequestPayload(joinPoint, signature);
        String requestPayloadJson = toJsonString(requestPayload);
        int requestSize = requestPayloadJson.getBytes().length;
        
        // MDC'ye request bilgilerini ekle (structured fields olarak)
        try {
            // Core tracking fields
            MDC.put("correlation_id", correlationId);
            MDC.put("session_id", sessionId);
            MDC.put("request_type", "REQUEST");
            
            // HTTP details
            MDC.put("http_method", httpMethod);
            MDC.put("request_uri", requestUri);
            MDC.put("full_uri", fullUri);
            MDC.put("client_ip", clientIp);
            MDC.put("user_agent", userAgent);
            
            // Application details
            MDC.put("controller", className);
            MDC.put("controller_method", methodName);
            MDC.put("endpoint", className + "." + methodName);
            
            // Request data
            MDC.put("request_payload", requestPayloadJson);
            MDC.put("request_size_bytes", String.valueOf(requestSize));
            
            // Request logu
            log.info("Incoming API Request");
            
            Object response = null;
            Exception exception = null;
            int httpStatusCode = 200;
            
            try {
                // Actual method execution
                response = joinPoint.proceed();
                return response;
            } catch (IllegalArgumentException e) {
                exception = e;
                httpStatusCode = 400; // Bad Request
                throw e;
            } catch (Exception e) {
                exception = e;
                httpStatusCode = 500; // Internal Server Error
                throw e;
            } finally {
                long executionTime = System.currentTimeMillis() - startTime;
                
                // MDC'yi response için güncelle
                MDC.put("request_type", "RESPONSE");
                MDC.put("execution_time_ms", String.valueOf(executionTime));
                MDC.put("http_status_code", String.valueOf(httpStatusCode));
                
                // Performance kategori
                String performanceCategory = categorizePerformance(executionTime);
                MDC.put("performance_category", performanceCategory);
                
                // Response logu
                if (exception != null) {
                    MDC.put("error_message", exception.getMessage());
                    MDC.put("error_type", exception.getClass().getSimpleName());
                    MDC.put("status", "ERROR");
                    
                    // Stack trace'i de ekle (ilk 3 satır)
                    StackTraceElement[] stackTrace = exception.getStackTrace();
                    if (stackTrace.length > 0) {
                        MDC.put("error_location", stackTrace[0].toString());
                    }
                    
                    log.error("API Request Failed");
                } else {
                    String responsePayloadJson = toJsonString(response);
                    int responseSize = responsePayloadJson.getBytes().length;
                    
                    MDC.put("response_payload", responsePayloadJson);
                    MDC.put("response_size_bytes", String.valueOf(responseSize));
                    MDC.put("status", "SUCCESS");
                    
                    log.info("API Request Completed");
                }
            }
        } finally {
            // MDC'yi temizle (memory leak olmasın)
            MDC.clear();
        }
    }
    
    /**
     * Performance kategorisi belirle (Kibana dashboard'ları için)
     */
    private String categorizePerformance(long executionTimeMs) {
        if (executionTimeMs < 100) return "FAST";
        if (executionTimeMs < 500) return "NORMAL";
        if (executionTimeMs < 1000) return "SLOW";
        return "VERY_SLOW";
    }

    /**
     * Request payload'ı extract et (RequestParam, RequestBody, PathVariable vs.)
     */
    private Map<String, Object> extractRequestPayload(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        Map<String, Object> payload = new HashMap<>();
        
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];
            
            // @RequestParam
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (requestParam != null) {
                String paramName = requestParam.name().isEmpty() ? requestParam.value() : requestParam.name();
                if (paramName.isEmpty()) {
                    paramName = parameter.getName();
                }
                payload.put(paramName, arg);
            }
            
            // @RequestBody
            RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
            if (requestBody != null) {
                payload.put("requestBody", arg);
            }
            
            // @PathVariable - isterseniz ekleyebiliriz
            // PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            // if (pathVariable != null) { ... }
        }
        
        // Eğer hiçbir annotation yoksa, tüm parametreleri ekle
        if (payload.isEmpty() && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                payload.put("arg" + i, args[i]);
            }
        }
        
        return payload;
    }

    /**
     * Mevcut HTTP Request'i al
     */
    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Client IP'sini al (Proxy arkasında bile çalışır)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Multiple IP'ler varsa ilkini al
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Object'i JSON string'e çevir
     */
    private String toJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}


