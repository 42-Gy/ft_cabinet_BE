package com.gyeongsan.cabinet.cabinet.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "CABINET")
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cabinet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "VISIBLE_NUM")
    private Integer visibleNum; // 사물함에 붙은 번호

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 32, nullable = false)
    private CabinetStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "LENT_TYPE", length = 16, nullable = false)
    private LentType lentType;

    @Column(name = "MAX_USER", nullable = false)
    private Integer maxUser;

    @Column(name = "STATUS_NOTE", length = 64)
    private String statusNote; // 고장 사유

    // --- 위치 정보 (직관적으로 포함) ---
    @Column(name = "FLOOR")
    private Integer floor;

    @Column(name = "SECTION")
    private String section;

    @Column(name = "GRID_ROW")
    private Integer row;

    @Column(name = "GRID_COL")
    private Integer col;

    // 👇 [수정] STATUS_NOTE 파라미터를 추가하여 모든 필드를 초기화합니다.
    protected Cabinet(Integer visibleNum, CabinetStatus status, LentType lentType, Integer maxUser,
                      String statusNote, Integer floor, String section, Integer row, Integer col) {
        this.visibleNum = visibleNum;
        this.status = status;
        this.lentType = lentType;
        this.maxUser = maxUser;
        this.statusNote = statusNote; // 필드 초기화
        this.floor = floor;
        this.section = section;
        this.row = row;
        this.col = col;
    }

    // 👇 [수정] Factory Method에도 STATUS_NOTE 파라미터를 추가합니다.
    public static Cabinet of(Integer visibleNum, CabinetStatus status, LentType lentType, Integer maxUser,
                             String statusNote, Integer floor, String section, Integer row, Integer col) {
        return new Cabinet(visibleNum, status, lentType, maxUser, statusNote, floor, section, row, col);
    }

    // 상태 변경
    public void updateStatus(CabinetStatus status) {
        this.status = status;
    }

    // 👇 [추가] AdminService에서 호출하는 상태 메모 업데이트 메서드
    public void updateStatusNote(String statusNote) {
        this.statusNote = statusNote;
    }
}