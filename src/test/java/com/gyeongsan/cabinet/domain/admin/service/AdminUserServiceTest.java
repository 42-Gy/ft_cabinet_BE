package com.gyeongsan.cabinet.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.PenaltyRequest;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private LentRepositoryPort lentRepository;
    @Mock private ItemHistoryRepositoryPort itemHistoryRepository;

    @InjectMocks private AdminUserService adminUserService;

    @Test
    @DisplayName("givePenalty: successfully gives penalty to user")
    void testGivePenalty() {
        User user = User.of("testUser", "test@example.com", null, UserRole.USER);
        given(userRepository.findByName("testUser")).willReturn(Optional.of(user));

        adminUserService.givePenalty("testUser", new PenaltyRequest(7, "Test penalty"));

        assertThat(user.getPenaltyDays()).isEqualTo(7);
    }

    @Test
    @DisplayName("givePenalty: throws when user not found")
    void testGivePenaltyUserNotFound() {
        given(userRepository.findByName("unknownUser")).willReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                adminUserService.givePenalty(
                                        "unknownUser", new PenaltyRequest(7, "Test")))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("deletePenalty: clears user penalty")
    void testDeletePenalty() {
        User user = User.of("testUser", "test@example.com", null, UserRole.USER);
        user.updatePenaltyDays(10);
        given(userRepository.findByName("testUser")).willReturn(Optional.of(user));

        adminUserService.deletePenalty("testUser");

        assertThat(user.getPenaltyDays()).isEqualTo(0);
    }

    @Test
    @DisplayName("promoteUserToAdmin: changes role to ADMIN")
    void testPromoteUserToAdmin() {
        User user = User.of("testUser", "test@example.com", null, UserRole.USER);
        given(userRepository.findByName("testUser")).willReturn(Optional.of(user));

        adminUserService.promoteUserToAdmin("testUser");

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("demoteUserToUser: changes role to USER")
    void testDemoteUserToUser() {
        User user = User.of("adminUser", "admin@example.com", null, UserRole.ADMIN);
        given(userRepository.findByName("adminUser")).willReturn(Optional.of(user));

        adminUserService.demoteUserToUser("adminUser");

        assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("updateUserLogtime: updates logtime")
    void testUpdateUserLogtime() {
        User user = User.of("testUser", "test@example.com", null, UserRole.USER);
        given(userRepository.findByName("testUser")).willReturn(Optional.of(user));

        adminUserService.updateUserLogtime("testUser", 500);

        assertThat(user.getMonthlyLogtime()).isEqualTo(500);
    }
}
