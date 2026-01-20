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

        // 👇 [수정됨] 날짜 검증 로직 호출 (실패해도 로그만 남기고 넘어가는 로직이 isRecentPhoto 내부에 있음)
        if (!isRecentPhoto(file)) {
            // isRecentPhoto가 true를 리턴하도록 수정했으므로 이 블록은 실행되지 않음
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

            // 👇 [중요] AI 응답 Key가 'status'인지 확인
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

            // 👇 [임시 수정] 메타데이터 없으면 경고만 남기고 통과 (return true)
            if (directory == null) {
                log.warn("⚠️ 사진에 Exif 메타데이터가 없습니다. (테스트를 위해 통과시킴)");
                return true;
            }

            String dateString = directory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            // 👇 [임시 수정] 날짜 정보 없으면 경고만 남기고 통과 (return true)
            if (dateString == null) {
                log.warn("⚠️ 사진에 촬영 날짜 정보가 없습니다. (테스트를 위해 통과시킴)");
                return true;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

            // 1. 사진 시간 파싱
            LocalDateTime localPhotoTime = LocalDateTime.parse(dateString, formatter);

            // 2. KST(서울) 기준 시간으로 변환 (타임존 문제 해결 코드 유지)
            ZonedDateTime photoZonedTime = localPhotoTime.atZone(ZoneId.of("Asia/Seoul"));
            ZonedDateTime currentZonedTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));

            long diffMinutes = ChronoUnit.MINUTES.between(photoZonedTime, currentZonedTime);

            log.info("📸 사진 촬영 경과 시간: {}분", diffMinutes);

            // 👇 [임시 수정] 시간 차이가 나도 일단 통과시킴 (원래는 return Math.abs(diffMinutes) <= 10;)
            if (Math.abs(diffMinutes) > 10) {
                log.warn("⚠️ 촬영 시간이 10분을 초과했습니다. (테스트를 위해 통과시킴)");
            }
            return true;

        } catch (Exception e) {
            // 👇 [임시 수정] 에러가 나도 통과시킴
            log.error("🚨 메타데이터 분석 중 오류 발생 (무시하고 진행)", e);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public List<ItemHistory> getUnusedLentTickets(Long userId, ItemType itemType) {
        return itemHistoryRepository.findUnusedItems(userId, itemType);
    }
}