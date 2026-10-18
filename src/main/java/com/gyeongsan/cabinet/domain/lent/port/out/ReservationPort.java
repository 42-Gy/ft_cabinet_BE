package com.gyeongsan.cabinet.domain.lent.port.out;

import com.gyeongsan.cabinet.domain.lent.model.ReservationOutcome;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface ReservationPort {

    ReservationOutcome reserveReplacing(Integer visibleNum, Long userId, long ttlMinutes);

    Optional<Long> getReservedUserId(Integer visibleNum);

    Map<Integer, Long> getReservedUserIds(Collection<Integer> visibleNums);

    Optional<Long> getCabinetReservationTtlSeconds(Integer visibleNum);

    Optional<Integer> getUserReservation(Long userId);

    Optional<Long> getUserReservationTtlSeconds(Long userId);

    void deleteReservation(Integer visibleNum, Long userId);

    Optional<Integer> cancelUserReservation(Long userId);
}
