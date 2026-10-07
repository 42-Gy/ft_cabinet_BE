package com.gyeongsan.cabinet.domain.cabinet.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
        name = "CABINET",
        indexes = {
            // 운영 DB 에 직접 걸려 있는 유니크 인덱스(2026-10-07)를 코드에도 기록한다. 이 인덱스가 없으면 번호로 거는 행 락
            // (findByVisibleNumWithLock)이 PK 순으로 테이블 전체를 훑으며 모든 행을 잠근다. 이름은 운영과 같게 맞춰,
            // ddl-auto=update 환경(테스트 서버)에서는 없을 때만 만들어지고 운영(validate)에는 영향이 없다.
            @Index(name = "idx_cabinet_visible_num", columnList = "VISIBLE_NUM", unique = true)
        })
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cabinet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "VISIBLE_NUM")
    private Integer visibleNum;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 32, nullable = false)
    private CabinetStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "LENT_TYPE", length = 16, nullable = false)
    private LentType lentType;

    @Column(name = "MAX_USER", nullable = false)
    private Integer maxUser;

    @Column(name = "STATUS_NOTE", length = 64)
    private String statusNote;

    @Column(name = "FLOOR")
    private Integer floor;

    @Column(name = "SECTION")
    private String section;

    @Column(name = "GRID_ROW")
    private Integer row;

    @Column(name = "GRID_COL")
    private Integer col;

    protected Cabinet(
            Integer visibleNum,
            CabinetStatus status,
            LentType lentType,
            Integer maxUser,
            String statusNote,
            Integer floor,
            String section,
            Integer row,
            Integer col) {
        this.visibleNum = visibleNum;
        this.status = status;
        this.lentType = lentType;
        this.maxUser = maxUser;
        this.statusNote = statusNote;
        this.floor = floor;
        this.section = section;
        this.row = row;
        this.col = col;
    }

    public static Cabinet of(
            Integer visibleNum,
            CabinetStatus status,
            LentType lentType,
            Integer maxUser,
            String statusNote,
            Integer floor,
            String section,
            Integer row,
            Integer col) {
        return new Cabinet(
                visibleNum, status, lentType, maxUser, statusNote, floor, section, row, col);
    }

    public void updateStatus(CabinetStatus status) {
        this.status = status;
    }

    public void updateStatusNote(String statusNote) {
        this.statusNote = statusNote;
    }

    public void updateStatus(CabinetStatus status, LentType lentType, String statusNote) {
        this.status = status;
        this.lentType = lentType;
        this.statusNote = statusNote;
    }

    public boolean isAvailable() {
        return this.status == CabinetStatus.AVAILABLE;
    }
}
