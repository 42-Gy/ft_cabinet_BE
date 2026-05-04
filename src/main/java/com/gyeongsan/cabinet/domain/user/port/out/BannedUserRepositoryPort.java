package com.gyeongsan.cabinet.domain.user.port.out;

import com.gyeongsan.cabinet.user.domain.BannedUser;

import java.util.List;
import java.util.Optional;

public interface BannedUserRepositoryPort {

    boolean existsByIntraId(String intraId);

    Optional<BannedUser> findByIntraId(String intraId);

    List<BannedUser> findAll();

    BannedUser save(BannedUser bannedUser);

    void delete(BannedUser bannedUser);
}
