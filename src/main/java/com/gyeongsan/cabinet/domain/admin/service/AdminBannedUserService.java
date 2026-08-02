package com.gyeongsan.cabinet.domain.admin.service;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.BannedUserResponse;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminBannedUserUseCase;
import com.gyeongsan.cabinet.domain.user.model.BannedUser;
import com.gyeongsan.cabinet.domain.user.port.out.BannedUserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminBannedUserService implements AdminBannedUserUseCase {

    private final BannedUserRepositoryPort bannedUserRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BannedUserResponse> getBannedUsers() {
        return bannedUserRepository.findAll().stream()
                .map(BannedUserResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public void addBannedUser(String intraId, String reason) {
        if (bannedUserRepository.existsByIntraId(intraId)) {
            throw new ServiceException(ErrorCode.USER_ALREADY_BANNED);
        }

        BannedUser bannedUser = BannedUser.of(intraId, reason);
        bannedUserRepository.save(bannedUser);
    }

    @Override
    public void removeBannedUser(String intraId) {
        BannedUser bannedUser =
                bannedUserRepository
                        .findByIntraId(intraId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.BANNED_USER_NOT_FOUND));

        bannedUserRepository.delete(bannedUser);
    }
}
