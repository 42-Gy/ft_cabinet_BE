package com.gyeongsan.cabinet.user.repository;

import com.gyeongsan.cabinet.user.domain.Attendance;
import com.gyeongsan.cabinet.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

        @Query("SELECT a FROM Attendance a WHERE a.user = :user AND a.attendanceDate BETWEEN :start AND :end")
        Optional<Attendance> findTodayAttendance(@Param("user") User user, @Param("start") LocalDate start,
                        @Param("end") LocalDate end);

        List<Attendance> findAllByUserId(Long userId);

        @Query("SELECT COUNT(a) FROM Attendance a WHERE a.user.id = :userId AND a.attendanceDate BETWEEN :start AND :end")
        Long countLoginDaysByUserId(@Param("userId") Long userId, @Param("start") LocalDate start,
                        @Param("end") LocalDate end);

        @Query("SELECT a.attendanceDate, COUNT(a) FROM Attendance a WHERE a.attendanceDate BETWEEN :start AND :end GROUP BY a.attendanceDate ORDER BY a.attendanceDate ASC")
        List<Object[]> getDailyAttendanceCounts(@Param("start") LocalDate start, @Param("end") LocalDate end);
}