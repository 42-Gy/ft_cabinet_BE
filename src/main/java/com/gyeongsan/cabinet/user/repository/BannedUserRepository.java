package com.gyeongsan.cabinet.user.repository;

import com.gyeongsan.cabinet.user.domain.BannedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BannedUserRepository extends JpaRepository<BannedUser, Long> {

    Optional<BannedUser> findByIntraId(String intraId);

    boolean existsByIntraId(String intraId);

    void deleteByIntraId(String intraId);
}
