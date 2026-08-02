package com.gyeongsan.cabinet.domain.admin.service;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.*;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminUserUseCase;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService implements AdminUserUseCase {

    private final UserRepositoryPort userRepository;
    private final LentRepositoryPort lentRepository;
    private final ItemHistoryRepositoryPort itemHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminAllUsersResponseDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());

        List<LentHistory> activeLents = lentRepository.findAllActiveLentByUserIds(userIds);
        Map<Long, LentHistory> lentMap =
                activeLents.stream()
                        .collect(Collectors.toMap(lh -> lh.getUser().getId(), lh -> lh));

        return users.map(
                user -> {
                    LentHistory activeLent = lentMap.get(user.getId());
                    Integer cabinetNum =
                            activeLent != null ? activeLent.getCabinet().getVisibleNum() : null;

                    return new AdminAllUsersResponseDto(
                            user.getId(),
                            user.getName(),
                            user.getEmail(),
                            user.getRole(),
                            user.getCoin(),
                            user.getPenaltyDays(),
                            user.getMonthlyLogtime(),
                            user.getBlackholedAt(),
                            activeLent != null,
                            cabinetNum);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(String name) {
        User user =
                userRepository
                        .findByName(name)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        Integer currentCabinetNum =
                lentRepository
                        .findByUserIdAndEndedAtIsNull(user.getId())
                        .map(lent -> lent.getCabinet().getVisibleNum())
                        .orElse(null);

        Map<String, Integer> itemCounts =
                itemHistoryRepository.findAllByUserIdAndUsedAtIsNull(user.getId()).stream()
                        .collect(
                                Collectors.groupingBy(
                                        itemHistory -> itemHistory.getItem().getType().name(),
                                        Collectors.collectingAndThen(
                                                Collectors.counting(), Long::intValue)));

        return AdminUserDetailResponse.of(user, currentCabinetNum, itemCounts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OverdueUserResponse> getOverdueUsers() {
        return lentRepository.findAllOverdueLentHistories(LocalDateTime.now()).stream()
                .map(OverdueUserResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PenaltyUserResponse> getPenaltyUsers() {
        return userRepository.findAllPenaltyUsers().stream()
                .map(PenaltyUserResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public void updateUserLogtime(String username, Integer newLogtime) {
        User user =
                userRepository
                        .findByName(username)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        user.updateMonthlyLogtime(newLogtime);
    }

    @Override
    public void givePenalty(String username, PenaltyRequest request) {
        User user =
                userRepository
                        .findByName(username)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        user.updatePenaltyDays(request.penaltyDays());
    }

    @Override
    public void deletePenalty(String username) {
        User user =
                userRepository
                        .findByName(username)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        user.clearPenalty();
    }

    @Override
    public void promoteUserToAdmin(String username) {
        User user =
                userRepository
                        .findByName(username)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
        user.updateRole(UserRole.ADMIN);
    }

    @Override
    public void demoteUserToUser(String username) {
        User user =
                userRepository
                        .findByName(username)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
        user.updateRole(UserRole.USER);
    }
}
