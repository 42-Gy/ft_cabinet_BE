package com.gyeongsan.cabinet.adapter.out.external.ft;

import com.gyeongsan.cabinet.domain.lent.port.out.FtApiPort;
import com.gyeongsan.cabinet.utils.FtApiManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FtApiAdapter implements FtApiPort {

    private final FtApiManager ftApiManager;

    @Override
    public int getLogtimeBetween(String intraId, LocalDateTime start, LocalDateTime end) {
        return ftApiManager.getLogtimeBetween(intraId, start, end);
    }
}
