package com.gyeongsan.cabinet.adapter.in.web.chatbot;

import com.gyeongsan.cabinet.adapter.in.web.chatbot.dto.ChatbotDtos.PersonalIntentItem;
import com.gyeongsan.cabinet.adapter.in.web.chatbot.dto.ChatbotDtos.PersonalRequest;
import com.gyeongsan.cabinet.adapter.in.web.chatbot.dto.ChatbotDtos.PersonalResponse;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;
import com.gyeongsan.cabinet.domain.chatbot.port.in.PersonalChatbotUseCase;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 챗봇의 "내 정보" 조회. 켜려면 CHATBOT_ENABLED 와 CHATBOT_PERSONAL_ENABLED 가 모두 true 여야 하고, 아니면 이 컨트롤러가 등록되지
 * 않는다(404).
 *
 * <p>보안 규칙: 조회 대상은 {@code @AuthenticationPrincipal} 의 사용자 하나뿐이다. 요청에서 사용자 ID 나 이름 같은 대상 지정 값을 받지
 * 않는다(받는 것은 인텐트 이름만). 응답은 캐시하지 않는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v4/chatbot/personal")
@RateLimiter(name = "chatbotApi")
@ConditionalOnProperty(
        name = {"app.chatbot.enabled", "app.chatbot.personal.enabled"},
        havingValue = "true")
public class PersonalChatbotController {

    private final PersonalChatbotUseCase personalChatbot;

    @PostMapping
    public ResponseEntity<ApiResponse<PersonalResponse>> answer(
            @RequestBody PersonalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            throw new ServiceException(ErrorCode.USER_NOT_FOUND);
        }
        PersonalIntent intent =
                PersonalIntent.fromName(request == null ? null : request.intent())
                        .orElseThrow(() -> new ServiceException(ErrorCode.CHATBOT_INVALID_INTENT));
        PersonalResponse body =
                PersonalResponse.of(personalChatbot.answer(principal.getUserId(), intent));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(body));
    }

    /** 칩으로 보여 줄 인텐트 목록(개인 정보 없음). 질문을 입력하지 않고 바로 고르는 버튼에 쓴다. */
    @GetMapping("/intents")
    public ApiResponse<List<PersonalIntentItem>> intents() {
        return ApiResponse.success(
                Arrays.stream(PersonalIntent.values())
                        .map(i -> new PersonalIntentItem(i.name(), i.label()))
                        .toList());
    }
}
