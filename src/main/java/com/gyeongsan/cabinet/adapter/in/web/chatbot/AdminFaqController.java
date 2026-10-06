package com.gyeongsan.cabinet.adapter.in.web.chatbot;

import com.gyeongsan.cabinet.adapter.in.web.chatbot.dto.AdminFaqDtos.FaqRequest;
import com.gyeongsan.cabinet.adapter.in.web.chatbot.dto.AdminFaqDtos.FaqResponse;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.common.dto.MessageResponse;
import com.gyeongsan.cabinet.domain.chatbot.port.in.FaqAdminUseCase;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FAQ 관리(관리자 전용: /v4/admin/** 는 ROLE_ADMIN 이 필요하다). 저장하면 챗봇 검색 색인이 자동으로 다시 만들어진다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v4/admin/faqs")
@RateLimiter(name = "userApi")
@ConditionalOnProperty(name = "app.chatbot.enabled", havingValue = "true")
public class AdminFaqController {

    private final FaqAdminUseCase faqAdmin;

    @GetMapping
    public ApiResponse<List<FaqResponse>> list() {
        return ApiResponse.success(faqAdmin.listAll().stream().map(FaqResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<FaqResponse> get(@PathVariable long id) {
        return ApiResponse.success(FaqResponse.from(faqAdmin.get(id)));
    }

    @PostMapping
    public ApiResponse<FaqResponse> create(@RequestBody FaqRequest request) {
        return ApiResponse.success(
                FaqResponse.from(
                        faqAdmin.create(
                                request.category(),
                                request.answer(),
                                request.resolvedEnabled(),
                                request.questions())));
    }

    @PutMapping("/{id}")
    public ApiResponse<FaqResponse> update(@PathVariable long id, @RequestBody FaqRequest request) {
        return ApiResponse.success(
                FaqResponse.from(
                        faqAdmin.update(
                                id,
                                request.category(),
                                request.answer(),
                                request.resolvedEnabled(),
                                request.questions())));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<MessageResponse> delete(@PathVariable long id) {
        faqAdmin.delete(id);
        return ApiResponse.success(new MessageResponse("✅ FAQ 가 삭제되었습니다."));
    }
}
