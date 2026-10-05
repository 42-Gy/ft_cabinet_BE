package com.gyeongsan.cabinet.domain.admin.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 관리자 작업이 건드린 대상 1건의 변경 전/후 스냅샷.
 *
 * <p>before/after 는 일반 Map 으로 들고, JSON 직렬화는 영속성 어댑터가 맡는다. 도메인은 직렬화 라이브러리를 모른다. before 는 나중에
 * 되돌리기(Undo)의 근거가 되므로 변경 전 값을 빠짐없이 담아야 한다.
 */
public record AdminActionLogItem(
        AdminActionTargetType targetType,
        Long targetId,
        String targetLabel,
        Map<String, Object> before,
        Map<String, Object> after) {

    public AdminActionLogItem {
        if (targetType == null || targetId == null) {
            throw new IllegalArgumentException("대상 종류와 ID 는 필수입니다.");
        }
        before = before == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(before));
        after = after == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(after));
    }
}
