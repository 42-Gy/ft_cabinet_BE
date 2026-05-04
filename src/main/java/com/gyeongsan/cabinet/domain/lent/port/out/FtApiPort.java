package com.gyeongsan.cabinet.domain.lent.port.out;

import java.time.LocalDateTime;

public interface FtApiPort {

    int getLogtimeBetween(String intraId, LocalDateTime start, LocalDateTime end);
}
