package com.gyeongsan.cabinet.domain.lent.port.in;

import org.springframework.web.multipart.MultipartFile;

public interface LentUseCase {

    void startLent(Long userId, Integer visibleNum);

    void checkLentCabinetImage(Long userId, MultipartFile file);

    void endLent(Long userId, String previousPassword, MultipartFile file, Boolean forceReturn, String reason);

    void useExtension(Long userId);

    void manualRenew(Long userId);

    void useSwap(Long userId, Integer newVisibleNum, String previousPassword, MultipartFile file, Boolean forceReturn, String reason);

    void usePenaltyExemption(Long userId);

    void processBlackholeReturn(Long userId);

    void updateAutoExtensionStatus(Long userId, Boolean enabled);

    void makeReservation(Long userId, Integer visibleNum);
}
