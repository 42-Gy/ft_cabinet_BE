package com.gyeongsan.cabinet.domain.lent.port.out;

import com.gyeongsan.cabinet.domain.user.model.FtCursusEntry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FtApiPort {

    int getLogtimeBetween(String intraId, LocalDateTime start, LocalDateTime end);

    /** 사용자의 cursus_users 를 조회한다. 호출이나 해석에 실패하면 비어 있는 Optional. */
    Optional<List<FtCursusEntry>> getCursusEntries(String intraId);
}
