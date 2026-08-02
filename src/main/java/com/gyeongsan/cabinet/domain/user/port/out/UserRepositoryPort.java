package com.gyeongsan.cabinet.domain.user.port.out;

import com.gyeongsan.cabinet.domain.user.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryPort {

    Optional<User> findById(Long id);

    Optional<User> findByIdWithLock(Long id);

    Optional<User> findByName(String name);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    Page<User> findAll(Pageable pageable);

    User save(User user);

    long count();

    long countByPenaltyDaysGreaterThan(Integer penaltyDays);

    Optional<Long> sumCoins();

    List<User> findAllPenaltyUsers();

    List<User> findAllBlackholedUsers(LocalDateTime date);
}
