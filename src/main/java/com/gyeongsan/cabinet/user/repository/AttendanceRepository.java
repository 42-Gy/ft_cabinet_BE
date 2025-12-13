package com.gyeongsan.cabinet.user.repository;

import com.gyeongsan.cabinet.user.domain.Attendance;
import com.gyeongsan.cabinet.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByUserAndAttendanceDate(User user, LocalDate date);
    List<Attendance> findAllByUserId(Long userId);
}