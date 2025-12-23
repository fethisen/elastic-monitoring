package com.elastic.monitoring.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@Order(10)
@RequiredArgsConstructor
public class ApiResponseLoggingAspect {
    private final ObjectMapper objectMapper;

    @Around("@within(restController)")
    public Object logResponse(ProceedingJoinPoint pjp, RestController restController) throws Throwable {
        long start = System.currentTimeMillis();

        Object result = pjp.proceed(); // controller çalışır, response üretilir

        long tookMs = System.currentTimeMillis() - start;

        String path = "unknown";
        String method = "unknown";
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            path = req.getRequestURI();
            method = req.getMethod();
        }

        Object body = unwrapBody(result);

        // İstersen burada endpoint whitelist koy (örn sadece /api/**)
        try {
            String json = objectMapper.writeValueAsString(body);
            log.info("api.response response={}", json);
        } catch (JsonProcessingException e) {
            log.warn("api.response response=<serialization_failed> type={}",
                     (body != null ? body.getClass().getName() : "null"));
        }

        return result; // response aynen geri döner
    }

    private Object unwrapBody(Object result) {
        if (result == null) return null;
        if (result instanceof ResponseEntity<?> re) return re.getBody();
        return result;
    }
}
