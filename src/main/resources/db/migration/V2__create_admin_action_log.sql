-- 관리자 작업 감사 로그. 작업 1회당 admin_action_log 1행, 작업이 건드린 대상마다 admin_action_log_item 1행.
-- V1 은 기존 스키마의 baseline(mysqldump --no-data)용으로 예약되어 있다. 기존 DB 에서는 baseline 을 V1 로
-- 먼저 찍은 뒤에 이 파일이 적용된다.
-- 테이블/컬럼명은 Spring Boot 기본 네이밍 전략(소문자 snake_case)이 만드는 물리 이름과 같아야 validate 를 통과한다.

CREATE TABLE admin_action_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    batch_id      VARCHAR(36)  NOT NULL,
    action_type   VARCHAR(48)  NOT NULL,
    actor_id      BIGINT       NOT NULL,
    actor_name    VARCHAR(32)  NOT NULL,
    reason        VARCHAR(255) NULL,
    request_json  MEDIUMTEXT   NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_action_log_batch_id (batch_id),
    KEY idx_admin_action_log_actor_created (actor_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE admin_action_log_item (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    log_id        BIGINT       NOT NULL,
    target_type   VARCHAR(32)  NOT NULL,
    target_id     BIGINT       NOT NULL,
    target_label  VARCHAR(64)  NULL,
    before_json   MEDIUMTEXT   NULL,
    after_json    MEDIUMTEXT   NULL,
    PRIMARY KEY (id),
    KEY idx_admin_action_log_item_target (target_type, target_id),
    CONSTRAINT fk_admin_action_log_item_log FOREIGN KEY (log_id) REFERENCES admin_action_log (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
