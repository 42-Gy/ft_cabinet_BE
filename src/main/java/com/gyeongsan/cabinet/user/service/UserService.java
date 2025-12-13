package com.gyeongsan.cabinet.user.service;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import com.gyeongsan.cabinet.user.domain.Attendance;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.dto.MyProfileResponseDto;
import com.gyeongsan.cabinet.user.repository.AttendanceRepository;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final LentRepository lentRepository;
    private final ItemHistoryRepository itemHistoryRepository;
    private final AttendanceRepository attendanceRepository;

    public MyProfileResponseDto getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        LentHistory activeLent = lentRepository.findByUserIdAndEndedAtIsNull(userId).orElse(null);
        List<ItemHistory> myItems = itemHistoryRepository.findAllByUserIdAndUsedAtIsNull(userId);

        if (myItems == null) {
            myItems = Collections.emptyList();
        }

        List<MyProfileResponseDto.MyItemDto> itemDtos = myItems.stream()
                .map(item -> {
                    String typeStr = (item.getItem() != null && item.getItem().getType() != null)
                            ? item.getItem().getType().name()
                            : "UNKNOWN";

                    return MyProfileResponseDto.MyItemDto.builder()
                            .itemHistoryId(item.getId())
                            .itemName(item.getItem() != null ? item.getItem().getName() : "알 수 없음")
                            .itemType(typeStr)
                            .purchaseAt(item.getPurchaseAt())
                            .build();
                })
                .collect(Collectors.toList());

        Long cabinetId = null;
        Integer visibleNum = null;
        String section = null;
        String lentStartedAt = null;

        if (activeLent != null && activeLent.getCabinet() != null) {
            Cabinet cabinet = activeLent.getCabinet();
            cabinetId = cabinet.getId();
            visibleNum = cabinet.getVisibleNum();
            section = cabinet.getSection();
            lentStartedAt = activeLent.getStartedAt().toString();
        }

        Integer penaltyDays = user.getPenaltyDays();
        if (penaltyDays == null) penaltyDays = 0;

        return MyProfileResponseDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .coin(user.getCoin())
                .penaltyDays(penaltyDays)
                .lentCabinetId(cabinetId)
                .visibleNum(visibleNum)
                .section(section)
                .lentStartedAt(lentStartedAt)
                .myItems(itemDtos)
                .build();
    }

    @Transactional
    public void doAttendance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        LocalDate today = LocalDate.now();

        if (attendanceRepository.existsByUserAndAttendanceDate(user, today)) {
            throw new IllegalStateException("이미 오늘 출석체크를 완료했습니다.");
        }

        Attendance attendance = new Attendance(user, today);
        attendanceRepository.save(attendance);

        user.addCoin(100L);
    }

    public List<LocalDate> getMyAttendanceDates(Long userId) {
        return attendanceRepository.findAllByUserId(userId).stream()
                .map(Attendance::getAttendanceDate)
                .collect(Collectors.toList());
    }
}
