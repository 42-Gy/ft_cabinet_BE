package com.gyeongsan.cabinet.domain.watermelon.port.out;

import com.gyeongsan.cabinet.domain.watermelon.domain.WatermelonEventLog;

import java.util.List;

public interface WatermelonEventLogRepositoryPort {
    WatermelonEventLog save(WatermelonEventLog log);
    List<WatermelonEventLog> findAllByUserId(Long userId);
}
