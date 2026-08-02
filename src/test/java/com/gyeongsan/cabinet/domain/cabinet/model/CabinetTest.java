package com.gyeongsan.cabinet.domain.cabinet.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CabinetTest {

    @Test
    @DisplayName("updateStatus: changes status, lentType, and statusNote")
    void testUpdateStatus() {
        Cabinet cabinet =
                Cabinet.of(1, CabinetStatus.AVAILABLE, LentType.PRIVATE, 1, "note", 1, "A", 1, 1);

        cabinet.updateStatus(CabinetStatus.BROKEN, LentType.PRIVATE, "Door is broken");

        assertThat(cabinet.getStatus()).isEqualTo(CabinetStatus.BROKEN);
        assertThat(cabinet.getLentType()).isEqualTo(LentType.PRIVATE);
        assertThat(cabinet.getStatusNote()).isEqualTo("Door is broken");
    }

    @Test
    @DisplayName("isAvailable: returns true when status is AVAILABLE")
    void testIsAvailable() {
        Cabinet cabinet =
                Cabinet.of(1, CabinetStatus.AVAILABLE, LentType.PRIVATE, 1, "note", 1, "A", 1, 1);
        assertThat(cabinet.isAvailable()).isTrue();

        cabinet.updateStatus(CabinetStatus.BROKEN, LentType.PRIVATE, null);
        assertThat(cabinet.isAvailable()).isFalse();
    }
}
