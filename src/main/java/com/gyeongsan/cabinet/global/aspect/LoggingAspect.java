package com.gyeongsan.cabinet.global.aspect;

import jakarta.servlet.http.HttpServletRequest;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 컨트롤러 호출의 요청/응답 시간을 기록한다.
 *
 * <p><b>요청 값은 남기지 않는다.</b> 예전에는 컨트롤러 인자를 {@code Arrays.toString} 으로 그대로 찍었는데, 요청 본문과 파라미터가 통째로
 * 로그(콘솔과 30일 보관되는 파일)에 들어갔다. 반납 공유 비밀번호({@code previousPassword})와 챗봇 질문 원문이 실제로 그렇게 남았다. 이런 값은 DTO
 * 의 필드이거나 문자열 파라미터라 이름으로 가려내기 어렵고, 새 엔드포인트가 생길 때마다 누가 가려야 하는 구조는 반드시 한 번은 빠뜨린다. 그래서 기본값을 "남기지 않음"으로
 * 뒤집었다: 숫자, 불리언, 열거형, 날짜/시각처럼 사용자 입력을 담을 수 없는 값만 그대로 남기고, 그 밖의 모든 인자(문자열, DTO, 파일, 컬렉션, 인증 정보)는 타입
 * 이름만 남긴다. 경로 변수는 요청 URI 에 이미 있어서 잃는 정보가 거의 없다.
 */
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
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();

        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        log.info(
                "👉 [REQUEST] {} {} | Params: {}",
                method,
                requestURI,
                summarize(joinPoint.getArgs()));

        Object result = joinPoint.proceed();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("✅ [RESPONSE] {} {} | Time: {}ms", method, requestURI, duration);

        if (duration > 2000) {
            log.warn("⚠️ [SLOW QUERY] {} took {}ms", requestURI, duration);
        }

        return result;
    }

    /** 컨트롤러 인자를 로그용으로 요약한다. 값을 남기는 것은 사용자 입력을 담을 수 없는 타입뿐이다. */
    static String summarize(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return Arrays.stream(args)
                .map(LoggingAspect::describe)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    static String describe(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof Number
                || arg instanceof Boolean
                || arg instanceof Enum<?>
                || arg instanceof Temporal) {
            return String.valueOf(arg);
        }
        // 문자열, DTO, 파일, 컬렉션, 인증 정보 등: 값은 남기지 않고 타입 이름만 남긴다.
        return arg.getClass().getSimpleName();
    }
}
