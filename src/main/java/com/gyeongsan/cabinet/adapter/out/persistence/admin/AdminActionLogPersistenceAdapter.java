package com.gyeongsan.cabinet.adapter.out.persistence.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLogItem;
import com.gyeongsan.cabinet.domain.admin.port.out.AdminActionLogPort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminActionLogPersistenceAdapter implements AdminActionLogPort {

    private final AdminActionLogJpaRepository adminActionLogJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(AdminActionLog log) {
        AdminActionLogEntity entity =
                new AdminActionLogEntity(
                        log.batchId(),
                        log.actionType(),
                        log.actor().id(),
                        log.actor().name(),
                        log.reason(),
                        toJson(log.request()),
                        log.createdAt());

        for (AdminActionLogItem item : log.items()) {
            entity.addItem(
                    new AdminActionLogItemEntity(
                            entity,
                            item.targetType(),
                            item.targetId(),
                            item.targetLabel(),
                            item.before() == null ? null : toJson(item.before()),
                            item.after() == null ? null : toJson(item.after())));
        }

        adminActionLogJpaRepository.save(entity);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            // 감사 기록을 못 남기면 그 작업도 롤백되어야 하므로 삼키지 않고 던진다.
            throw new IllegalStateException("감사 로그 직렬화에 실패했습니다.", e);
        }
    }
}
