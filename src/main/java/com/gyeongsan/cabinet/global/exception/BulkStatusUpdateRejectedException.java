package com.gyeongsan.cabinet.global.exception;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.BulkStatusRejection;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 사물함 일괄 변경이 거부되었음을 알린다. 거부된 요청은 아무 변경도 남기지 않는다. */
@Getter
public class BulkStatusUpdateRejectedException extends RuntimeException {

    private final transient BulkStatusRejection rejection;

    public BulkStatusUpdateRejectedException(BulkStatusRejection rejection) {
        super(buildMessage(rejection));
        this.rejection = rejection;
    }

    /** 존재하지 않는 ID 가 하나라도 있으면 404, 대여 중인 사물함 때문이면 409. */
    public HttpStatus getStatus() {
        return rejection.missingCabinetIds().isEmpty() ? HttpStatus.CONFLICT : HttpStatus.NOT_FOUND;
    }

    private static String buildMessage(BulkStatusRejection rejection) {
        StringBuilder message = new StringBuilder("일괄 변경이 거부되었습니다. 아무 것도 변경되지 않았습니다.");
        if (!rejection.missingCabinetIds().isEmpty()) {
            message.append(" 존재하지 않는 사물함 ID: ").append(rejection.missingCabinetIds()).append('.');
        }
        if (!rejection.occupiedCabinets().isEmpty()) {
            String visibleNums =
                    rejection.occupiedCabinets().stream()
                            .map(o -> String.valueOf(o.visibleNum()))
                            .distinct()
                            .collect(Collectors.joining(", "));
            message.append(" 대여 중인 사물함: [")
                    .append(visibleNums)
                    .append("] (대여를 종료하려면 endActiveLents=true 로 명시하세요).");
        }
        return message.toString();
    }
}
