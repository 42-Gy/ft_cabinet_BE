package com.gyeongsan.cabinet.user.repository;

import com.gyeongsan.cabinet.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByName(String name);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.blackholedAt < :now AND u.deletedAt IS NULL")
    List<User> findAllBlackholedUsers(@Param("now") LocalDateTime now);
}
