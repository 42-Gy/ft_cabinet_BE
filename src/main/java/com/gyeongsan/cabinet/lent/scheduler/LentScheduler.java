package com.gyeongsan.cabinet.lent.scheduler;

import com.gyeongsan.cabinet.alarm.dto.AlarmEvent;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class LentScheduler {

    private final LentRepository lentRepository;
    private final ApplicationEventPublisher eventPublisher; // ⭕ 이벤트 발행기 주입

    /**
     * 연체 감지 스케줄러
     * cron = "0 0 0 * * *" -> 매일 자정 (실제 배포용)
     * 테스트할 때는 "0 * * * * *" (매 분 0초)로 바꿔서 쓰세요!
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void checkOverdue() {
        log.info("⏰ 연체자 단속 시작! (현재 시각: {})", LocalDateTime.now());

        // 1. 지금보다 기한이 지난 대여 기록 찾기
        List<LentHistory> overdueLents = lentRepository.findAllOverdueLentHistories(LocalDateTime.now());

        if (overdueLents.isEmpty()) {
            log.info(" - 다행히 연체자가 없습니다.");
            return;
        }

        // 2. 연체 처리 및 알림 발송
        for (LentHistory lh : overdueLents) {
            // 이미 OVERDUE 상태면 패스
            if (lh.getCabinet().getStatus() == CabinetStatus.OVERDUE) {
                continue;
            }

            // (1) 사물함 상태 강제 변경 (DB 작업)
            lh.getCabinet().updateStatus(CabinetStatus.OVERDUE);

            // (2) 알림 이벤트 발행 (비동기 처리 위임)
            String userEmail = lh.getUser().getEmail(); // 유저 이메일 가져오기

            // 메시지 내용 작성
            String message = String.format("🚨 *[연체 경고]*\n%s님, %d번 사물함이 연체되었습니다. 즉시 반납해주세요!",
                    lh.getUser().getName(), lh.getCabinet().getId());

            // 👉 여기서 "쪽지(Event)"를 던집니다! (받는 사람이 알아서 처리함)
            eventPublisher.publishEvent(new AlarmEvent(userEmail, message));

            log.info("📨 연체 알림 이벤트 발행 완료: {}", userEmail);
        }
    }
}