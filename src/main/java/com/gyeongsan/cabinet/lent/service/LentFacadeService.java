package com.gyeongsan.cabinet.lent.service;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class LentFacadeService {

    private final UserRepository userRepository;
    private final CabinetRepository cabinetRepository;
    private final LentRepository lentRepository;
    private final ItemHistoryRepository itemHistoryRepository;
    private final TransactionTemplate transactionTemplate;
    private final RestTemplate restTemplate;

    @Value("${cabinet.policy.lent-term}")
    private int lentTerm;

    @Value("${cabinet.policy.extension-term}")
    private long extensionTerm;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Transactional
    public void startLentCabinet(Long userId, Integer visibleNum) {
        log.info("대여 시도 - User: {}, Cabinet Num: {}", userId, visibleNum);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (user.getPenaltyDays() > 0) {
            throw new ServiceException(ErrorCode.PENALTY_USER);
        }

        Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        if (lentRepository.findByUserIdAndEndedAtIsNull(userId).isPresent()) {
            throw new ServiceException(ErrorCode.LENT_ALREADY_EXIST);
        }

        LocalDateTime blackholedAt = user.getBlackholedAt();
        if (blackholedAt != null && blackholedAt.isBefore(LocalDateTime.now().plusDays(3))) {
            throw new ServiceException(ErrorCode.BLACKHOLED_USER);
        }

        if (cabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
        }

        List<ItemHistory> lentTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.LENT);

        if (lentTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.LENT_TICKET_NOT_FOUND);
        }

        ItemHistory ticket = lentTickets.get(0);
        ticket.use();

        cabinet.updateStatus(CabinetStatus.FULL);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusDays(lentTerm);

        LentHistory lentHistory = LentHistory.of(user, cabinet, now, expiredAt);
        lentRepository.save(lentHistory);

        log.info("대여 성공! 대여 ID: {}", lentHistory.getId());
    }

    public void endLentCabinetWithAi(Long userId, String shareCode, MultipartFile cabinetImage) {
        log.info("AI 반납 시도 - User: {}, Memo: {}", userId, shareCode);

        checkCabinetStatusViaAi(cabinetImage);

        transactionTemplate.execute(status -> {
            processReturnTransaction(userId, shareCode);
            return null;
        });
    }

    protected void processReturnTransaction(Long userId, String shareCode) {
        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        Cabinet cabinet = lentHistory.getCabinet();
        lentHistory.endLent(LocalDateTime.now(), shareCode);

        if (cabinet.getStatus() == CabinetStatus.FULL) {
            cabinet.updateStatus(CabinetStatus.AVAILABLE);
        }

        log.info("반납 성공! 대여 ID: {}, 사물함: {}, 공유 비번: {}",
                lentHistory.getId(), cabinet.getVisibleNum(), shareCode);
    }

    private void checkCabinetStatusViaAi(MultipartFile image) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(aiServerUrl, requestEntity, Map.class);

            Map<String, Object> result = response.getBody();
            if (result == null) {
                throw new RuntimeException("AI 응답이 비어있습니다.");
            }

            String status = (String) result.get("status");
            log.info("🤖 AI 판독 결과: {}", status);

            if ("OCCUPIED".equals(status)) {
                throw new ServiceException(ErrorCode.CABINET_NOT_EMPTY);
            }

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 서버 통신 오류: {}", e.getMessage());
            throw new ServiceException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    @Transactional
    public void useExtension(Long userId) {
        log.info("연장권 사용 시도 - User: {}", userId);

        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        List<ItemHistory> extensionTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.EXTENSION);

        if (extensionTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.EXTENSION_TICKET_NOT_FOUND);
        }

        ItemHistory ticket = extensionTickets.get(0);
        ticket.use();

        lentHistory.extendExpiration(extensionTerm);

        log.info("연장 성공! 변경된 만료일: {}", lentHistory.getExpiredAt());
    }

    @Transactional
    public void useSwap(Long userId, Integer newVisibleNum, String password) {
        log.info("이사권 사용 시도 - User: {}, NewCabinet Num: {}, OldCabinet Password: {}", userId, newVisibleNum, password);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        LentHistory oldLent = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        if (oldLent.getCabinet().getVisibleNum().equals(newVisibleNum)) {
            throw new ServiceException(ErrorCode.SAME_CABINET_SWAP);
        }

        Cabinet newCabinet = cabinetRepository.findByVisibleNumWithLock(newVisibleNum)
                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        if (newCabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
        }

        List<ItemHistory> swapTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.SWAP);

        if (swapTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.SWAP_TICKET_NOT_FOUND);
        }

        ItemHistory ticket = swapTickets.get(0);
        ticket.use();

        Cabinet oldCabinet = oldLent.getCabinet();

        oldLent.endLent(LocalDateTime.now(), password);

        if (oldCabinet.getStatus() == CabinetStatus.FULL) {
            oldCabinet.updateStatus(CabinetStatus.AVAILABLE);
        }

        newCabinet.updateStatus(CabinetStatus.FULL);

        LentHistory newLent = LentHistory.of(user, newCabinet, LocalDateTime.now(), oldLent.getExpiredAt());
        lentRepository.save(newLent);

        log.info("이사 성공! 🚚 Old: {} -> New: {}", oldCabinet.getVisibleNum(), newCabinet.getVisibleNum());
    }

    @Transactional
    public void usePenaltyExemption(Long userId) {
        log.info("패널티 감면권 사용 시도 - User: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (user.getPenaltyDays() <= 0) {
            throw new ServiceException(ErrorCode.PENALTY_NOT_FOUND);
        }

        List<ItemHistory> penaltyTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.PENALTY_EXEMPTION);

        if (penaltyTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.PENALTY_EXEMPTION_TICKET_NOT_FOUND);
        }

        ItemHistory ticket = penaltyTickets.get(0);
        ticket.use();

        int newPenalty = user.getPenaltyDays() - 2;
        user.updatePenaltyDays(newPenalty);

        log.info("감면 성공! 패널티: {}일 -> {}일", newPenalty + 2, user.getPenaltyDays());
    }
}