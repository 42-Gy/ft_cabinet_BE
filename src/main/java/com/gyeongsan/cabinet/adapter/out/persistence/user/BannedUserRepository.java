package com.gyeongsan.cabinet.adapter.out.persistence.user;

import com.gyeongsan.cabinet.domain.user.model.BannedUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BannedUserRepository extends JpaRepository<BannedUser, Long> {

    Optional<BannedUser> findByIntraId(String intraId);

    boolean existsByIntraId(String intraId);

    void deleteByIntraId(String intraId);
}
