package com.gyeongsan.cabinet.lent.service;

import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class ItemCheckService {

    private final ItemHistoryRepository itemHistoryRepository;
    private final WebClient webClient;

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    public boolean checkItem(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("❌ AI 검사 실패: 사진 파일이 없습니다.");
            throw new ServiceException(ErrorCode.INVALID_IMAGE);
        }

        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());

            Map response = webClient.post()
                    .uri(aiServerUrl + "/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("🤖 AI Server Response: {}", response);

            if (response != null && response.containsKey("status")) {
                String statusValue = String.valueOf(response.get("status")).toUpperCase();

                switch (statusValue) {
                    case "EMPTY":
                        return true;

                    case "OCCUPIED":
                        log.warn("❌ 물품 감지됨 (FULL)");
                        throw new ServiceException(ErrorCode.CABINET_NOT_EMPTY);

                    case "INVALID":
                        log.warn("❌ 잘못된 사진 형식 (INVALID)");
                        throw new ServiceException(ErrorCode.INVALID_IMAGE);

                    default:
                        log.warn("⚠️ 알 수 없는 상태 값: {}", statusValue);
                        throw new ServiceException(ErrorCode.AI_SERVER_ERROR);
                }
            }
            return false;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("🚨 AI 서버 통신 오류: ", e);
            throw new ServiceException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public List<ItemHistory> getUnusedLentTickets(Long userId, ItemType itemType) {
        return itemHistoryRepository.findUnusedItems(userId, itemType);
    }
}