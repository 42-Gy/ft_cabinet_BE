package com.gyeongsan.cabinet.adapter.out.persistence.alarm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminReportRecipientAdapterTest {

    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final AdminReportRecipientAdapter adapter = new AdminReportRecipientAdapter(users);

    @Test
    @DisplayName("ADMIN 과 MASTER 두 권한만 조회하고, 유저의 인트라 ID(name)를 중복 없이 돌려준다")
    void queriesAdminAndMasterOnly() {
        when(users.findAllActiveByRoleIn(anyCollection()))
                .thenReturn(
                        List.of(
                                User.of("admin1", "a@x.kr", UserRole.ADMIN),
                                User.of("master1", "m@x.kr", UserRole.MASTER),
                                User.of("admin1", "a2@x.kr", UserRole.ADMIN)));

        List<String> ids = adapter.findRecipientIntraIds();

        assertThat(ids).containsExactly("admin1", "master1");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UserRole>> roles = ArgumentCaptor.forClass(Collection.class);
        verify(users).findAllActiveByRoleIn(roles.capture());
        assertThat(roles.getValue()).containsExactlyInAnyOrder(UserRole.ADMIN, UserRole.MASTER);
    }

    @Test
    @DisplayName("해당 권한 유저가 없으면 빈 목록이다")
    void emptyWhenNoAdmins() {
        when(users.findAllActiveByRoleIn(anyCollection())).thenReturn(List.of());

        assertThat(adapter.findRecipientIntraIds()).isEmpty();
    }
}
