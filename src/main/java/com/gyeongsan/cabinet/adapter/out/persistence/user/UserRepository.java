package com.gyeongsan.cabinet.adapter.out.persistence.user;

import com.gyeongsan.cabinet.domain.user.model.User;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") Long id);

    Optional<User> findByName(String name);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.blackholedAt < :date AND u.deletedAt IS NULL")
    List<User> findAllBlackholedUsers(@Param("date") LocalDateTime date);

    long countByPenaltyDaysGreaterThan(Integer penaltyDays);

    @Query("SELECT SUM(u.coin) FROM User u")
    Optional<Long> sumCoins();

    @Query("SELECT u FROM User u WHERE u.penaltyDays > 0 ORDER BY u.penaltyDays DESC")
    List<User> findAllPenaltyUsers();
}
