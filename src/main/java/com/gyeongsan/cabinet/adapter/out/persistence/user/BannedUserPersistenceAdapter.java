package com.gyeongsan.cabinet.adapter.out.persistence.user;

import com.gyeongsan.cabinet.domain.user.port.out.BannedUserRepositoryPort;
import com.gyeongsan.cabinet.user.domain.BannedUser;
import com.gyeongsan.cabinet.user.repository.BannedUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BannedUserPersistenceAdapter implements BannedUserRepositoryPort {

    private final BannedUserRepository bannedUserRepository;

    @Override
    public boolean existsByIntraId(String intraId) {
        return bannedUserRepository.existsByIntraId(intraId);
    }

    @Override
    public Optional<BannedUser> findByIntraId(String intraId) {
        return bannedUserRepository.findByIntraId(intraId);
    }

    @Override
    public List<BannedUser> findAll() {
        return bannedUserRepository.findAll();
    }

    @Override
    public BannedUser save(BannedUser bannedUser) {
        return bannedUserRepository.save(bannedUser);
    }

    @Override
    public void delete(BannedUser bannedUser) {
        bannedUserRepository.delete(bannedUser);
    }
}
