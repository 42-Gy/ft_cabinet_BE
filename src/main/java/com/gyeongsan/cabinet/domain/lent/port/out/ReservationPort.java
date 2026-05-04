package com.gyeongsan.cabinet.domain.lent.port.out;

import java.util.Optional;

public interface ReservationPort {

    void reserve(Integer visibleNum, Long userId, long ttlMinutes);

    Optional<Long> getReservedUserId(Integer visibleNum);

    Optional<Integer> getUserReservation(Long userId);

    void deleteReservation(Integer visibleNum, Long userId);
}
