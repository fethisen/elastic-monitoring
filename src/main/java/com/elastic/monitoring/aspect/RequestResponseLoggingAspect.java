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
        
        // HTTP Request bilgilerini al
        HttpServletRequest request = getCurrentHttpRequest();
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String requestUri = request != null ? request.getRequestURI() : "UNKNOWN";
        String clientIp = request != null ? getClientIp(request) : "UNKNOWN";
        
        // Method bilgilerini al
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        
        // Request payload'ı hazırla
        Map<String, Object> requestPayload = extractRequestPayload(joinPoint, signature);
        
        // MDC'ye request bilgilerini ekle (structured fields olarak)
        try {
            MDC.put("http_method", httpMethod);
            MDC.put("request_uri", requestUri);
            MDC.put("client_ip", clientIp);
            MDC.put("controller", className);
            MDC.put("controller_method", methodName);
            MDC.put("request_payload", toJsonString(requestPayload));
            
            // Request logu - kısa ve öz mesaj
            log.info("Incoming API Request");
            
            Object response = null;
            Exception exception = null;
            
            try {
                // Actual method execution
                response = joinPoint.proceed();
                return response;
            } catch (Exception e) {
                exception = e;
                throw e;
            } finally {
                long executionTime = System.currentTimeMillis() - startTime;
                
                // MDC'ye response bilgilerini ekle
                MDC.put("execution_time_ms", String.valueOf(executionTime));
                
                // Response logu
                if (exception != null) {
                    MDC.put("error_message", exception.getMessage());
                    MDC.put("status", "ERROR");
                    log.error("API Request Failed");
                } else {
                    MDC.put("response_payload", toJsonString(response));
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


