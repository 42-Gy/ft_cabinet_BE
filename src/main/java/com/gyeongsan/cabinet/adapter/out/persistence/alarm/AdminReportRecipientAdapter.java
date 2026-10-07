package com.gyeongsan.cabinet.adapter.out.persistence.alarm;

import com.gyeongsan.cabinet.domain.alarm.port.out.ReportRecipientPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 오류제보 수신자 = 권한이 ADMIN 또는 MASTER 인 유저 전원(탈퇴한 유저 제외). 권한이 바뀌면 다음 전달부터 바로 반영되도록 매번 DB 에서 읽는다. 슬랙 ID
 * 매핑은 AlarmPort(SlackBotService)가 인트라 ID(User.name)로 한다.
 */
@Component
@RequiredArgsConstructor
public class AdminReportRecipientAdapter implements ReportRecipientPort {

    private static final Set<UserRole> RECIPIENT_ROLES = Set.of(UserRole.ADMIN, UserRole.MASTER);

    private final UserRepositoryPort userRepository;

    @Override
    public List<String> findRecipientIntraIds() {
        return userRepository.findAllActiveByRoleIn(RECIPIENT_ROLES).stream()
                .map(User::getName)
                .distinct()
                .toList();
    }
}
