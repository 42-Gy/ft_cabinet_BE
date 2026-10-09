package com.gyeongsan.cabinet.global.exception;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoRejection;
import java.util.stream.Collectors;
import lombok.Getter;

/** Undo 가 거부되었음을 알린다(409). 거부된 요청은 아무 변경도 남기지 않는다. */
@Getter
public class UndoRejectedException extends RuntimeException {

    private final transient UndoRejection rejection;

    public UndoRejectedException(UndoRejection rejection) {
        super(buildMessage(rejection));
        this.rejection = rejection;
    }

    private static String buildMessage(UndoRejection rejection) {
        String codes =
                rejection.conflicts().stream()
                        .map(c -> c.code().name())
                        .distinct()
                        .collect(Collectors.joining(", "));
        return "되돌리기가 거부되었습니다. 아무 것도 변경되지 않았습니다. 충돌 "
                + rejection.conflicts().size()
                + "건: ["
                + codes
                + "]";
    }
}
