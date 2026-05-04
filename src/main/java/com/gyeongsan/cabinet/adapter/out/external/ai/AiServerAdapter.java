package com.gyeongsan.cabinet.adapter.out.external.ai;

import com.gyeongsan.cabinet.domain.lent.port.out.AiCheckPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class AiServerAdapter implements AiCheckPort {

    private final WebClient webClient;

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    @Override
    public boolean checkItem(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("AI 검사 실패: 사진 파일이 없습니다.");
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

            log.info("AI Server Response: {}", response);

            if (response != null && response.containsKey("status")) {
                String statusValue = String.valueOf(response.get("status")).toUpperCase();

                switch (statusValue) {
                    case "EMPTY":
                        return true;
                    case "OCCUPIED":
                        throw new ServiceException(ErrorCode.CABINET_NOT_EMPTY);
                    case "INVALID":
                        throw new ServiceException(ErrorCode.INVALID_IMAGE);
                    default:
                        throw new ServiceException(ErrorCode.AI_SERVER_ERROR);
                }
            }
            return false;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 서버 통신 오류: ", e);
            throw new ServiceException(ErrorCode.AI_SERVER_ERROR);
        }
    }
}
