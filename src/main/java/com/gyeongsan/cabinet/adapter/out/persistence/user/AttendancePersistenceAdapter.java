package com.gyeongsan.cabinet.adapter.out.persistence.user;

import com.gyeongsan.cabinet.domain.user.port.out.AttendanceRepositoryPort;
import com.gyeongsan.cabinet.user.domain.Attendance;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AttendancePersistenceAdapter implements AttendanceRepositoryPort {

    private final AttendanceRepository attendanceRepository;

    @Override
    public Attendance save(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }

    @Override
    public Optional<Attendance> findTodayAttendance(User user, LocalDate start, LocalDate end) {
        return attendanceRepository.findTodayAttendance(user, start, end);
    }

    @Override
    public long countLoginDaysByUserId(Long userId, LocalDate start, LocalDate end) {
        return attendanceRepository.countLoginDaysByUserId(userId, start, end);
    }

    @Override
    public List<Attendance> findAllByUserId(Long userId) {
        return attendanceRepository.findAllByUserId(userId);
    }

    @Override
    public List<Object[]> getDailyAttendanceCounts(LocalDate start, LocalDate end) {
        return attendanceRepository.getDailyAttendanceCounts(start, end);
    }
}
