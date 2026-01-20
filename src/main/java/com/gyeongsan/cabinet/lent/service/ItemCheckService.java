package com.gyeongsan.cabinet.lent.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
            return false;
        }

        if (!isRecentPhoto(file)) {
            log.warn("❌ 사진 검증 실패: 촬영 후 10분이 지났거나 메타데이터가 없습니다.");
            return false;
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
                String statusValue = String.valueOf(response.get("status"));
                return "EMPTY".equalsIgnoreCase(statusValue);
            }

            return false;

        } catch (Exception e) {
            log.error("🚨 AI 서버 통신 오류: ", e);
            return false;
        }
    }

    private boolean isRecentPhoto(MultipartFile file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.getInputStream());
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            if (directory == null) {
                log.warn("⚠️ 사진에 Exif 메타데이터가 없습니다. (스크린샷 의심)");
                return false;
            }

            String dateString = directory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (dateString == null) {
                log.warn("⚠️ 사진에 촬영 날짜 정보가 없습니다.");
                return false;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

            LocalDateTime localPhotoTime = LocalDateTime.parse(dateString, formatter);

            ZonedDateTime photoZonedTime = localPhotoTime.atZone(ZoneId.of("Asia/Seoul"));
            ZonedDateTime currentZonedTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));

            long diffMinutes = ChronoUnit.MINUTES.between(photoZonedTime, currentZonedTime);

            log.info("📸 사진 촬영 경과 시간: {}분", diffMinutes);

            return Math.abs(diffMinutes) <= 10;

        } catch (Exception e) {
            log.error("🚨 메타데이터 분석 중 오류 발생", e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<ItemHistory> getUnusedLentTickets(Long userId, ItemType itemType) {
        return itemHistoryRepository.findUnusedItems(userId, itemType);
    }
}
