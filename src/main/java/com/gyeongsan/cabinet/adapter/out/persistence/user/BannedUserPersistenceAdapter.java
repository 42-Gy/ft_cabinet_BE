package com.gyeongsan.cabinet.adapter.out.persistence.user;

import com.gyeongsan.cabinet.domain.user.model.BannedUser;
import com.gyeongsan.cabinet.domain.user.port.out.BannedUserRepositoryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
