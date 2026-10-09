package com.gyeongsan.cabinet.adapter.out.external.ft;

import com.gyeongsan.cabinet.domain.lent.port.out.FtApiPort;
import com.gyeongsan.cabinet.domain.user.model.FtCursusEntry;
import com.gyeongsan.cabinet.utils.FtApiManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FtApiAdapter implements FtApiPort {

    private final FtApiManager ftApiManager;

    @Override
    public int getLogtimeBetween(String intraId, LocalDateTime start, LocalDateTime end) {
        return ftApiManager.getLogtimeBetween(intraId, start, end);
    }

    @Override
    public Optional<List<FtCursusEntry>> getCursusEntries(String intraId) {
        return ftApiManager.getCursusEntries(intraId);
    }
}
