package com.gyeongsan.cabinet.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.gyeongsan.cabinet.domain.user.model.FtCursusEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 42 API 의 cursus_users 를 {@link FtCursusEntry} 목록으로 바꾼다. 로그인(/v2/me, Map)과 지급일
 * 재조회(/v2/users/:login, JsonNode)가 같은 규칙을 쓰도록 한 곳에 둔다.
 *
 * <p>반환값이 비어 있으면 "해석 실패"(cursus_users 가 없거나 배열이 아님)이고, 빈 목록이면 "항목 없음"이다. 둘은 구분해서 다뤄야 한다. 형식이 깨진 개별
 * 항목은 건너뛴다.
 */
public final class FtCursusParser {

    private FtCursusParser() {}

    public static Optional<List<FtCursusEntry>> fromAttribute(Object cursusUsers) {
        if (!(cursusUsers instanceof List<?> list)) {
            return Optional.empty();
        }
        List<FtCursusEntry> entries = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> cursusUser)) {
                continue;
            }
            if (!(cursusUser.get("cursus") instanceof Map<?, ?> cursus)
                    || !(cursus.get("id") instanceof Number id)) {
                continue;
            }
            Object grade = cursusUser.get("grade");
            entries.add(new FtCursusEntry(id.intValue(), grade instanceof String s ? s : null));
        }
        return Optional.of(entries);
    }

    public static Optional<List<FtCursusEntry>> fromJson(JsonNode cursusUsers) {
        if (cursusUsers == null || !cursusUsers.isArray()) {
            return Optional.empty();
        }
        List<FtCursusEntry> entries = new ArrayList<>();
        for (JsonNode cursusUser : cursusUsers) {
            JsonNode id = cursusUser.path("cursus").path("id");
            if (!id.canConvertToInt() || !id.isNumber()) {
                continue;
            }
            JsonNode grade = cursusUser.path("grade");
            entries.add(
                    new FtCursusEntry(id.intValue(), grade.isTextual() ? grade.asText() : null));
        }
        return Optional.of(entries);
    }
}
