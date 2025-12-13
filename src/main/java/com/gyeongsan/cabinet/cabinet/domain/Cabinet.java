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

    protected Cabinet(Integer visibleNum, CabinetStatus status, LentType lentType, Integer maxUser,
                      String statusNote, Integer floor, String section, Integer row, Integer col) {
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

    public static Cabinet of(Integer visibleNum, CabinetStatus status, LentType lentType, Integer maxUser,
                             String statusNote, Integer floor, String section, Integer row, Integer col) {
        return new Cabinet(visibleNum, status, lentType, maxUser, statusNote, floor, section, row, col);
    }

    public void updateStatus(CabinetStatus status) {
        this.status = status;
    }

    public void updateStatusNote(String statusNote) {
        this.statusNote = statusNote;
    }
}
