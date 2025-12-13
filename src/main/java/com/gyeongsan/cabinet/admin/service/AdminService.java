package com.gyeongsan.cabinet.admin.service;

import com.gyeongsan.cabinet.admin.dto.AdminUserDetailResponse;
import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final CabinetRepository cabinetRepository;
    private final UserRepository userRepository;

    @Transactional
    public void updateCabinetStatus(Long cabinetId, CabinetStatus newStatus, String statusNote) {
        Cabinet cabinet = cabinetRepository.findById(cabinetId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사물함입니다."));

        cabinet.updateStatus(newStatus);
        cabinet.updateStatusNote(statusNote);
    }

    @Transactional(readOnly = true)
    public List<AdminUserDetailResponse> findAllUsers() {
        return userRepository.findAll().stream()
                .map(AdminUserDetailResponse::from)
                .collect(Collectors.toList());
    }
}
