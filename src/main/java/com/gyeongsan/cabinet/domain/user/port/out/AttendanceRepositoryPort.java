package com.gyeongsan.cabinet.domain.user.port.out;

import com.gyeongsan.cabinet.user.domain.Attendance;
import com.gyeongsan.cabinet.user.domain.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepositoryPort {

    Attendance save(Attendance attendance);

    Optional<Attendance> findTodayAttendance(User user, LocalDate start, LocalDate end);

    long countLoginDaysByUserId(Long userId, LocalDate start, LocalDate end);

    List<Attendance> findAllByUserId(Long userId);

    List<Object[]> getDailyAttendanceCounts(LocalDate start, LocalDate end);
}
