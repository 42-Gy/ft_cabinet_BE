package com.gyeongsan.cabinet.adapter.out.persistence.user;

import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByIdWithLock(Long id) {
        return userRepository.findByIdWithLock(id);
    }

    @Override
    public Optional<User> findByName(String name) {
        return userRepository.findByName(name);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public long count() {
        return userRepository.count();
    }

    @Override
    public long countByPenaltyDaysGreaterThan(Integer penaltyDays) {
        return userRepository.countByPenaltyDaysGreaterThan(penaltyDays);
    }

    @Override
    public Optional<Long> sumCoins() {
        return userRepository.sumCoins();
    }

    @Override
    public List<User> findAllPenaltyUsers() {
        return userRepository.findAllPenaltyUsers();
    }

    @Override
    public List<User> findAllBlackholedUsers(LocalDateTime date) {
        return userRepository.findAllBlackholedUsers(date);
    }
}
