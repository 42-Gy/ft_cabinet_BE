package com.gyeongsan.cabinet.domain.user.model;

/**
 * cursus_users 를 해석한 결과.
 *
 * @param parsed 응답을 정상적으로 해석했는지. false 면 저장된 grade 를 건드리지 않아야 한다
 * @param grade 본과정(cursus 21)의 grade 원문. 본과정 항목이 없거나 값이 없으면 null
 * @param rejected 값이 있었지만 형식이 올바르지 않아 버렸는지(저장하지 않고 경고만 남긴다)
 */
public record FtGradeSnapshot(boolean parsed, String grade, boolean rejected) {

    private static final FtGradeSnapshot UNPARSED = new FtGradeSnapshot(false, null, false);

    public static FtGradeSnapshot unparsed() {
        return UNPARSED;
    }

    public static FtGradeSnapshot of(String grade) {
        return new FtGradeSnapshot(true, grade, false);
    }

    public static FtGradeSnapshot ofRejected() {
        return new FtGradeSnapshot(true, null, true);
    }

    /** 저장된 grade 를 이 값으로 바꿔도 되는지. 해석에 실패했거나 형식이 올바르지 않으면 기존 값을 유지한다. */
    public boolean shouldStore() {
        return parsed && !rejected;
    }
}
