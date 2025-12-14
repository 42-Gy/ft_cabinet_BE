package com.gyeongsan.cabinet.admin.service;

import com.gyeongsan.cabinet.admin.dto.*;
import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class AdminService {

    private final UserRepository userRepository;
    private final CabinetRepository cabinetRepository;
    private final LentRepository lentRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        long bannedUserCount = userRepository.countByPenaltyDaysGreaterThan(0);

        return new AdminDashboardResponse(
                userRepository.count(),
                cabinetRepository.count(),
                lentRepository.countByEndedAtIsNull(),
                cabinetRepository.countByStatus(com.gyeongsan.cabinet.cabinet.domain.CabinetStatus.BROKEN),
                bannedUserCount
        );
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(String name) {
        User user = userRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다: " + name));

        return AdminUserDetailResponse.from(user);
    }

    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        user.unban();
        log.info("[Admin] 유저({}) 밴 해제 완료", userId);
    }

    public void provideCoin(Long userId, CoinProvideRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        user.addCoin(request.amount());
        log.info("[Admin] 유저({})에게 코인 {}개 지급. 사유: {}", userId, request.amount(), request.reason());
    }

    public void updateCabinetStatus(Long cabinetId, CabinetStatusRequest request) {
        Cabinet cabinet = cabinetRepository.findById(cabinetId)
                .orElseThrow(() -> new IllegalArgumentException("사물함이 없습니다."));

        cabinet.updateStatus(request.status(), request.lentType(), request.statusNote());
        log.info("[Admin] 사물함({}) 상태 변경: {}, {}, {}", cabinetId, request.status(), request.lentType(), request.statusNote());
    }

    public void forceReturn(Long cabinetId) {
        LentHistory activeLent = lentRepository.findByCabinetIdAndEndedAtIsNull(cabinetId)
                .orElseThrow(() -> new IllegalArgumentException("현재 대여 중인 사물함이 아닙니다."));

        activeLent.endLent(LocalDateTime.now());
        log.info("[Admin] 사물함({}) 강제 반납 처리 완료", cabinetId);
    }
}
