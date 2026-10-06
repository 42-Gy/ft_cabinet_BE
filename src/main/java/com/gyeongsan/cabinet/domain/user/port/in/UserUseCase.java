package com.gyeongsan.cabinet.domain.user.port.in;

import com.gyeongsan.cabinet.adapter.in.web.user.dto.MyProfileResponseDto;
import com.gyeongsan.cabinet.domain.item.model.Item;
import com.gyeongsan.cabinet.domain.user.model.FtGradeSnapshot;
import java.time.LocalDate;
import java.util.List;

public interface UserUseCase {

    MyProfileResponseDto getMyProfile(Long userId);

    void doAttendance(Long userId);

    List<LocalDate> getMyAttendanceDates(Long userId);

    void processLogtimeTransaction(
            Long userId, Item lentTicketItem, int totalMinutes, boolean isPayDay);

    /** 지급일에 grade 를 다시 조회해야 하는 사용자인지(새 기준의 영향을 받는 로그타임 구간의 비-트센 사용자). */
    boolean needsGradeRefresh(Long userId, int totalMinutes);

    /** 다시 조회한 grade 를 저장한다. 해석에 실패한 결과(parsed=false)는 저장하지 않는다. */
    void updateFtGrade(Long userId, FtGradeSnapshot snapshot);
}
