package com.gyeongsan.cabinet.domain.user.model;

import java.util.List;
import java.util.Optional;

/**
 * 42 API cursus_users 에서 본과정(cursus 21)의 grade 를 골라낸다.
 *
 * <p>한 사용자의 cursus_users 에는 피시너(9, grade "Pisciner"), 본과정(21), 재도전 피시너(66) 가 함께 들어 있다. 다른 cursus 의
 * grade 를 읽으면 오판하므로 반드시 21번 항목만 본다. 실제 응답으로 확인된 grade 는 "Cadet", "Transcender" 뿐이다.
 */
public final class FtGradeResolver {

    public static final int CURSUS_42_ID = 21;
    public static final String TRANSCENDER = "Transcender";
    public static final String CADET = "Cadet";

    /** USER.FT_GRADE 컬럼 길이. */
    static final int MAX_GRADE_LENGTH = 32;

    private FtGradeResolver() {}

    /**
     * @param entries 해석에 실패했다면 비어 있는 Optional
     */
    public static FtGradeSnapshot resolve(Optional<List<FtCursusEntry>> entries) {
        if (entries == null || entries.isEmpty()) {
            return FtGradeSnapshot.unparsed();
        }

        String firstGrade = null;
        boolean found = false;
        for (FtCursusEntry entry : entries.get()) {
            if (entry == null || entry.cursusId() != CURSUS_42_ID) {
                continue;
            }
            // 본과정 항목이 여러 개여도 하나라도 Transcender 면 트센이다.
            if (TRANSCENDER.equals(entry.grade())) {
                return FtGradeSnapshot.of(TRANSCENDER);
            }
            if (!found) {
                firstGrade = entry.grade();
                found = true;
            }
        }

        if (firstGrade == null || firstGrade.isEmpty()) {
            return FtGradeSnapshot.of(null);
        }
        if (!isStorable(firstGrade)) {
            return FtGradeSnapshot.ofRejected();
        }
        return FtGradeSnapshot.of(firstGrade);
    }

    /** 지금까지 실제 응답으로 확인된 값(또는 값 없음)인지. 아니면 운영 중 등급 체계 변경을 알아채도록 경고 대상이 된다. */
    public static boolean isRecognized(String grade) {
        return grade == null || CADET.equals(grade) || TRANSCENDER.equals(grade);
    }

    private static boolean isStorable(String grade) {
        if (grade.length() > MAX_GRADE_LENGTH || grade.isBlank()) {
            return false;
        }
        return grade.chars().noneMatch(Character::isISOControl);
    }
}
