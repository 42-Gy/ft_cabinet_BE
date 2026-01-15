package com.gyeongsan.cabinet.admin.service;

import com.gyeongsan.cabinet.admin.dto.*;
import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.dto.CabinetPendingResponseDto;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.repository.ItemRepository;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class AdminService {

        private final UserRepository userRepository;
        private final CabinetRepository cabinetRepository;
        private final LentRepository lentRepository;
        private final ItemRepository itemRepository;

        @Transactional(readOnly = true)
        public AdminDashboardResponse getDashboard() {
                long bannedUserCount = userRepository.countByPenaltyDaysGreaterThan(0);

                return new AdminDashboardResponse(
                                userRepository.count(),
                                cabinetRepository.count(),
                                lentRepository.countByEndedAtIsNull(),
                                cabinetRepository.countByStatus(CabinetStatus.BROKEN),
                                bannedUserCount);
        }

        @Transactional(readOnly = true)
        public AdminUserDetailResponse getUserDetail(String name) {
                User user = userRepository.findByName(name)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

                Integer currentCabinetNum = lentRepository.findByUserIdAndEndedAtIsNull(user.getId())
                                .map(lent -> lent.getCabinet().getVisibleNum())
                                .orElse(null);

                return AdminUserDetailResponse.of(user, currentCabinetNum);
        }

        @Transactional(readOnly = true)
        public Page<CabinetHistoryResponse> getCabinetHistory(Integer visibleNum, Pageable pageable) {
                cabinetRepository.findByVisibleNumWithLock(visibleNum)
                                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

                return lentRepository.findHistoryByCabinet(visibleNum, pageable)
                                .map(CabinetHistoryResponse::from);
        }

        public void updateUserLogtime(String username, Integer newLogtime) {
                User user = userRepository.findByName(username)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

                user.updateMonthlyLogtime(newLogtime);
                log.info("[Admin] 유저({}) 로그타임 수정 완료: {}분", user.getName(), newLogtime);
        }

        public void provideCoin(String username, CoinProvideRequest request) {
                User user = userRepository.findByName(username)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

                user.addCoin(request.amount());
                log.info(
                                "[Admin] 유저({})에게 코인 {}개 지급. 사유: {}",
                                user.getName(),
                                request.amount(),
                                request.reason());
        }

        public void updateCabinetStatus(Integer visibleNum, CabinetStatusRequest request) {
                Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

                cabinet.updateStatus(
                                request.status(),
                                request.lentType(),
                                request.statusNote());

                log.info(
                                "[Admin] 사물함({}) 상태 변경: {}, {}, {}",
                                visibleNum,
                                request.status(),
                                request.lentType(),
                                request.statusNote());
        }

        public void forceReturn(Integer visibleNum) {
                Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

                LentHistory activeLent = lentRepository.findByCabinetIdAndEndedAtIsNull(cabinet.getId())
                                .orElse(null);

                if (activeLent != null) {
                        activeLent.endLent(LocalDateTime.now(), "관리자 강제 반납");
                }

                cabinet.updateStatus(
                                CabinetStatus.PENDING,
                                cabinet.getLentType(),
                                "강제 반납: 물품 수거 및 청소 필요");

                log.info("[Admin] 사물함({}) 강제 반납 완료 -> 상태: PENDING", visibleNum);
        }

        @Transactional(readOnly = true)
        public List<CabinetPendingResponseDto> getPendingCabinets() {
                List<Cabinet> pendingCabinets = cabinetRepository.findAllByStatus(CabinetStatus.PENDING);

                return pendingCabinets.stream()
                                .map(cabinet -> new CabinetPendingResponseDto(
                                                cabinet.getVisibleNum(),
                                                cabinet.getStatusNote(),
                                                cabinet.getLentType()))
                                .collect(Collectors.toList());
        }

        public void approveManualReturn(Integer visibleNum) {
                Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

                if (cabinet.getStatus() != CabinetStatus.PENDING) {
                        throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
                }

                cabinet.updateStatus(
                                CabinetStatus.AVAILABLE,
                                cabinet.getLentType(),
                                null);

                log.info("[Admin] 사물함({}) 수동 반납 승인 완료 -> AVAILABLE", visibleNum);
        }

        public void updateItemPrice(String itemName, Long newPrice) {
                Item item = itemRepository.findByName(itemName)
                                .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND));

                if (newPrice < 0) {
                        throw new IllegalArgumentException("가격은 0원 이상이어야 합니다.");
                }

                Long oldPrice = item.getPrice();
                item.updatePrice(newPrice);

                log.info(
                                "[Admin] 아이템({}) 가격 변경 완료: {} -> {}",
                                item.getName(),
                                oldPrice,
                                newPrice);
        }

        @Transactional(readOnly = true)
        public List<OverdueUserResponse> getOverdueUsers() {
                return lentRepository.findAllOverdueLentHistories(LocalDateTime.now())
                                .stream()
                                .map(OverdueUserResponse::from)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public CabinetDetailResponse getCabinetDetail(Integer visibleNum) {
                Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

                LentHistory activeLent = lentRepository.findByCabinetIdAndEndedAtIsNull(cabinet.getId())
                                .orElse(null);

                return CabinetDetailResponse.of(cabinet, activeLent);
        }
}
