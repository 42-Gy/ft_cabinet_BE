-- 챗봇 FAQ. 답변(faq) 하나에 같은 뜻의 질문 표현(faq_question)이 여러 개 달린다.
-- 임베딩 벡터는 저장하지 않는다(서버가 시작할 때와 FAQ 가 바뀔 때 메모리에서 계산한다).
CREATE TABLE faq (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    -- 초기 데이터로 들어온 항목의 고정 키. 여러 서버가 동시에 처음 기동해도 같은 항목이 두 번 들어가지 않게 한다. 직접 추가한 항목은 NULL.
    seed_key   VARCHAR(60)   NULL,
    category   VARCHAR(30)   NOT NULL,
    answer     VARCHAR(2000) NOT NULL,
    enabled    BIT(1)        NOT NULL,
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_faq_seed_key (seed_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE faq_question (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    faq_id     BIGINT       NOT NULL,
    sort_order INT          NOT NULL,
    question   VARCHAR(200) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_faq_question_faq_id (faq_id),
    CONSTRAINT fk_faq_question_faq FOREIGN KEY (faq_id) REFERENCES faq (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
