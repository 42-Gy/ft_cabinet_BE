package com.gyeongsan.cabinet.admin.service;

import com.gyeongsan.cabinet.admin.dto.AdminUserDetailResponse; // DTO import
import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.user.repository.UserRepository; // UserRepository import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List; // List import
import java.util.stream.Collectors; // Collectors import

@Service
@RequiredArgsConstructor
public class AdminService {

    private final CabinetRepository cabinetRepository;
    private final UserRepository userRepository; // [추가] 사용자 정보 조회를 위해 주입

    /**
     * 1. 특정 사물함의 상태를 강제로 변경 (운영용)
     */
    @Transactional
    public void updateCabinetStatus(Long cabinetId, CabinetStatus newStatus, String statusNote) {
        Cabinet cabinet = cabinetRepository.findById(cabinetId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사물함입니다."));

        // 상태 변경 및 메모 기록 (Cabinet 엔티티에 메서드 추가 완료됨)
        cabinet.updateStatus(newStatus);
        cabinet.updateStatusNote(statusNote);
    }

    /**
     * 2. [추가] 모든 사용자 목록을 조회하고 DTO로 변환 (관리자 페이지용)
     */
    @Transactional(readOnly = true)
    public List<AdminUserDetailResponse> findAllUsers() {
        // 모든 유저를 조회한 후, AdminUserDetailResponse DTO로 변환하여 반환합니다.
        return userRepository.findAll().stream()
                .map(AdminUserDetailResponse::from)
                .collect(Collectors.toList());
    }
}