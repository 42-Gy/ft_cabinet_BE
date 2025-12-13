package com.gyeongsan.cabinet.global.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
@Log4j2
public class LoggingAspect {

    @Pointcut("execution(* com.gyeongsan.cabinet..*Controller.*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        String params = Arrays.toString(joinPoint.getArgs());
        if (params.length() > 150) {
            params = params.substring(0, 150) + "... (생략됨)";
        }

        log.info("👉 [REQUEST] {} {} | Params: {}", method, requestURI, params);

        Object result = joinPoint.proceed();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("✅ [RESPONSE] {} {} | Time: {}ms", method, requestURI, duration);

        if (duration > 2000) {
            log.warn("⚠️ [SLOW QUERY] {} took {}ms", requestURI, duration);
        }

        return result;
    }
}
