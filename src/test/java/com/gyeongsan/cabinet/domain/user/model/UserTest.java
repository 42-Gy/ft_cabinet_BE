package com.gyeongsan.cabinet.domain.user.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("updatePenaltyDays: adds penalty days")
    void testUpdatePenaltyDays() {
        User user = User.of("testUser", "test@example.com", null, UserRole.USER);
        assertThat(user.getPenaltyDays()).isEqualTo(0);

        user.updatePenaltyDays(3);
        assertThat(user.getPenaltyDays()).isEqualTo(3);

        user.applyPenalty(2);
        assertThat(user.getPenaltyDays()).isEqualTo(5);
    }

    @Test
    @DisplayName("clearPenalty: sets penalty days to 0")
    void testClearPenalty() {
        User user = User.of("testUser", "test@example.com", null, UserRole.USER);
        user.updatePenaltyDays(5);

        user.clearPenalty();

        assertThat(user.getPenaltyDays()).isEqualTo(0);
    }

    @Test
    @DisplayName("addCoin: increases coin balance")
    void testAddCoin() {
        User user = User.of("testUser", "test@example.com", null, UserRole.USER);
        assertThat(user.getCoin()).isEqualTo(0L);

        user.addCoin(100L);
        assertThat(user.getCoin()).isEqualTo(100L);
    }

    @Test
    @DisplayName("useCoin: decreases coin balance")
    void testUseCoin() {
        User user = User.of("testUser", "test@example.com", null, UserRole.USER);
        user.addCoin(100L);

        user.useCoin(30L);
        assertThat(user.getCoin()).isEqualTo(70L);
    }

    @Test
    @DisplayName("updateMonthlyLogtime: updates logtime")
    void testUpdateMonthlyLogtime() {
        User user = User.of("testUser", "test@example.com", null, UserRole.USER);

        user.updateMonthlyLogtime(120);

        assertThat(user.getMonthlyLogtime()).isEqualTo(120);
    }

    @Test
    @DisplayName("updateRole: changes user role")
    void testUpdateRole() {
        User user = User.of("testUser", "test@example.com", null, UserRole.USER);

        user.updateRole(UserRole.ADMIN);

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("decayPenalty: decreases penalty days")
    void testDecayPenalty() {
        User user = User.of("testUser", "test@example.com", null, UserRole.USER);
        user.applyPenalty(5);

        user.decayPenalty();

        assertThat(user.getPenaltyDays()).isEqualTo(4);
    }
}
