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

@Aspect // 1. 이 클래스는 AOP 관점(Aspect)입니다.
@Component // 2. 스프링 빈으로 등록합니다.
@Log4j2
public class LoggingAspect {

    /**
     * 포인트컷 설정: com.gyeongsan.cabinet 패키지 하위의 모든 Controller를 대상으로 함
     */
    @Pointcut("execution(* com.gyeongsan.cabinet..*Controller.*(..))")
    public void controllerMethods() {}

    /**
     * 어드바이스 설정: 대상 메서드 실행 전/후에 개입 (Around)
     */
    @Around("controllerMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 요청 시작 시간 측정
        long startTime = System.currentTimeMillis();

        // 2. 현재 요청 정보(Request) 가져오기
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String method = request.getMethod(); // GET, POST 등
        String requestURI = request.getRequestURI(); // /v4/lent/cabinets/1

        // 👇 [수정] 파라미터가 너무 길면 잘라서 출력합니다. (가독성 향상)
        String params = Arrays.toString(joinPoint.getArgs());
        if (params.length() > 150) {
            params = params.substring(0, 150) + "... (생략됨)";
        }

        log.info("👉 [REQUEST] {} {} | Params: {}", method, requestURI, params);

        // 3. 실제 타겟 메서드(Controller) 실행
        Object result = joinPoint.proceed();

        // 4. 요청 종료 시간 및 소요 시간 계산
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("✅ [RESPONSE] {} {} | Time: {}ms", method, requestURI, duration);

        // 5. 실행 시간이 너무 길면(예: 2초 이상) 경고 로그 출력 (성능 모니터링 기초)
        if (duration > 2000) {
            log.warn("⚠️ [SLOW QUERY] {} took {}ms", requestURI, duration);
        }

        return result;
    }
}