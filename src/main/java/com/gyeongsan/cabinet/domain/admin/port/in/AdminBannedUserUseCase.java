package com.gyeongsan.cabinet.domain.admin.port.in;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.BannedUserResponse;
import java.util.List;

public interface AdminBannedUserUseCase {
    List<BannedUserResponse> getBannedUsers();

    void addBannedUser(String intraId, String reason);

    void removeBannedUser(String intraId);
}
