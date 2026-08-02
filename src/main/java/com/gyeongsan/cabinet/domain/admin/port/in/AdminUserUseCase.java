package com.gyeongsan.cabinet.domain.admin.port.in;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.*;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserUseCase {
    Page<AdminAllUsersResponseDto> getAllUsers(Pageable pageable);

    AdminUserDetailResponse getUserDetail(String name);

    List<OverdueUserResponse> getOverdueUsers();

    List<PenaltyUserResponse> getPenaltyUsers();

    void updateUserLogtime(String username, Integer newLogtime);

    void givePenalty(String username, PenaltyRequest request);

    void deletePenalty(String username);

    void promoteUserToAdmin(String username);

    void demoteUserToUser(String username);
}
