package com.gyeongsan.cabinet.domain.lent.port.out;

import com.gyeongsan.cabinet.domain.lent.model.ReservationOutcome;
import java.util.Optional;

public interface ReservationPort {

    void reserve(Integer visibleNum, Long userId, long ttlMinutes);

    /**
     * 사물함을 예약한다. 한 사용자는 예약을 하나만 가질 수 있어서, 다른 사물함을 이미 예약 중이면 그 예약을 취소하고 새로 예약한다. 확인과 변경이 한 번에 일어나므로,
     * 같은 사용자가 서로 다른 사물함을 동시에 예약해도 예약은 항상 하나만 남는다.
     */
    ReservationOutcome reserveReplacing(Integer visibleNum, Long userId, long ttlMinutes);

    Optional<Long> getReservedUserId(Integer visibleNum);

    Optional<Integer> getUserReservation(Long userId);

    /** 내 예약의 남은 시간(초). 예약이 없으면 비어 있다. */
    Optional<Long> getUserReservationTtlSeconds(Long userId);

    /**
     * 사물함을 대여한 뒤 예약을 정리한다. 대여한 사물함이 내 예약이면 지우고, 내가 다른 사물함을 예약하고 있었다면 그 예약도 함께 지운다. 남의 예약은 지우지 않는다.
     */
    void deleteReservation(Integer visibleNum, Long userId);

    /** 내 예약을 취소하고, 취소한 사물함 번호를 돌려준다. 예약이 없으면 비어 있다. */
    Optional<Integer> cancelUserReservation(Long userId);
}
