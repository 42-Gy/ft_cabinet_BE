package com.gyeongsan.cabinet.domain.lent.port.in;

import org.springframework.web.multipart.MultipartFile;

public interface LentUseCase {

    void startLent(Long userId, Integer visibleNum);

    void checkLentCabinetImage(Long userId, MultipartFile file);

    void endLent(
            Long userId,
            String previousPassword,
            MultipartFile file,
            Boolean forceReturn,
            String reason);

    void useExtension(Long userId);

    void manualRenew(Long userId);

    void useSwap(
            Long userId,
            Integer newVisibleNum,
            String previousPassword,
            MultipartFile file,
            Boolean forceReturn,
            String reason);

    void usePenaltyExemption(Long userId);

    void updateAutoExtensionStatus(Long userId, Boolean enabled);

    void makeReservation(Long userId, Integer visibleNum);

    /** 내 예약을 취소하고, 취소한 사물함 번호를 돌려준다. 예약이 없으면 RESERVATION_NOT_FOUND. */
    Integer cancelReservation(Long userId);
}
