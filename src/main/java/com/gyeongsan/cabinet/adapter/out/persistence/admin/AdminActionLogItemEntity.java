package com.gyeongsan.cabinet.adapter.out.persistence.admin;

import com.gyeongsan.cabinet.domain.admin.model.AdminActionTargetType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 테이블 정의는 db/migration/V2__create_admin_action_log.sql 이 단일 출처이며, 이름은 거기와 같게 유지한다. */
@Entity
@Table(
        name = "ADMIN_ACTION_LOG_ITEM",
        indexes = {
            @Index(name = "idx_admin_action_log_item_target", columnList = "TARGET_TYPE, TARGET_ID")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminActionLogItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "LOG_ID",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_admin_action_log_item_log"))
    private AdminActionLogEntity log;

    @Enumerated(EnumType.STRING)
    @Column(name = "TARGET_TYPE", nullable = false, length = 32)
    private AdminActionTargetType targetType;

    @Column(name = "TARGET_ID", nullable = false)
    private Long targetId;

    @Column(name = "TARGET_LABEL", length = 64)
    private String targetLabel;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "BEFORE_JSON", columnDefinition = "MEDIUMTEXT")
    private String beforeJson;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "AFTER_JSON", columnDefinition = "MEDIUMTEXT")
    private String afterJson;

    AdminActionLogItemEntity(
            AdminActionLogEntity log,
            AdminActionTargetType targetType,
            Long targetId,
            String targetLabel,
            String beforeJson,
            String afterJson) {
        this.log = log;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetLabel = targetLabel;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
    }
}
