package com.gyeongsan.cabinet.admin.service;

import com.gyeongsan.cabinet.admin.dto.*;
import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
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
                cabinetRepository.countByStatus(CabinetStatus.BROKEN),
                bannedUserCount
        );
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(String name) {
        User user = userRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다: " + name));

        return AdminUserDetailResponse.from(user);
    }

    public void updateUserLogtime(String username, Integer newLogtime) {
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다: " + username));

        user.updateMonthlyLogtime(newLogtime);
        log.info("[Admin] 유저({}) 로그타임 수정 완료: {}분", user.getName(), newLogtime);
    }

    public void provideCoin(String username, CoinProvideRequest request) {
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다: " + username));

        user.addCoin(request.amount());
        log.info("[Admin] 유저({})에게 코인 {}개 지급. 사유: {}", user.getName(), request.amount(), request.reason());
    }

    public void updateCabinetStatus(Integer visibleNum, CabinetStatusRequest request) {
        Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사물함 번호입니다: " + visibleNum));

        cabinet.updateStatus(request.status(), request.lentType(), request.statusNote());
        log.info("[Admin] 사물함({}) 상태 변경: {}, {}, {}", visibleNum, request.status(), request.lentType(), request.statusNote());
    }

    public void forceReturn(Integer visibleNum) {
        Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사물함 번호입니다: " + visibleNum));

        LentHistory activeLent = lentRepository.findByCabinetIdAndEndedAtIsNull(cabinet.getId())
                .orElse(null);

        if (activeLent != null) {
            activeLent.endLent(LocalDateTime.now(), "관리자 강제 반납");
        }

        cabinet.updateStatus(
                CabinetStatus.PENDING,
                cabinet.getLentType(),
                "강제 반납: 물품 수거 및 청소 필요"
        );

        log.info("[Admin] 사물함({}) 강제 반납 완료 -> 상태: PENDING", visibleNum);
    }
}