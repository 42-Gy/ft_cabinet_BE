-- Undo 기록이 어떤 작업을 되돌렸는지 가리키는 컬럼. 일반 작업 기록은 NULL 이다.
-- UNIQUE 로 한 작업은 한 번만 되돌릴 수 있도록 DB 수준에서 보장한다(NULL 은 여러 개 허용).
ALTER TABLE admin_action_log
    ADD COLUMN undo_of_batch_id VARCHAR(36) NULL,
    ADD UNIQUE KEY uk_admin_action_log_undo_of_batch_id (undo_of_batch_id);
