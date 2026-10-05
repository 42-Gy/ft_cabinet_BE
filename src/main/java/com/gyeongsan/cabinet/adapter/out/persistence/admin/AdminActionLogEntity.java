package com.gyeongsan.cabinet.adapter.out.persistence.admin;

import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 테이블 정의는 db/migration/V2__create_admin_action_log.sql 이 단일 출처이며, 이름은 거기와 같게 유지한다. */
@Entity
@Table(
        name = "ADMIN_ACTION_LOG",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_admin_action_log_batch_id", columnNames = "BATCH_ID")
        },
        indexes = {
            @Index(name = "idx_admin_action_log_actor_created", columnList = "ACTOR_ID, CREATED_AT")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminActionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "BATCH_ID", nullable = false, length = 36)
    private String batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACTION_TYPE", nullable = false, length = 48)
    private AdminActionType actionType;

    @Column(name = "ACTOR_ID", nullable = false)
    private Long actorId;

    @Column(name = "ACTOR_NAME", nullable = false, length = 32)
    private String actorName;

    @Column(name = "REASON")
    private String reason;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "REQUEST_JSON", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String requestJson;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "log", cascade = CascadeType.PERSIST)
    private List<AdminActionLogItemEntity> items = new ArrayList<>();

    AdminActionLogEntity(
            String batchId,
            AdminActionType actionType,
            Long actorId,
            String actorName,
            String reason,
            String requestJson,
            LocalDateTime createdAt) {
        this.batchId = batchId;
        this.actionType = actionType;
        this.actorId = actorId;
        this.actorName = actorName;
        this.reason = reason;
        this.requestJson = requestJson;
        this.createdAt = createdAt;
    }

    void addItem(AdminActionLogItemEntity item) {
        items.add(item);
    }
}
