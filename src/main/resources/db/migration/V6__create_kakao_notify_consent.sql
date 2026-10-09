-- 슬랙 공지 → 카카오톡 "나에게 보내기" 알림.
-- 카카오 talk_message 동의 정보를 로그인용 oauth_link 와 분리해 따로 둔다(oauth_link 는 건드리지 않는다).
-- refresh_token 은 AES-GCM 으로 암호화한 값만 저장한다. access_token 은 저장하지 않는다.
ALTER TABLE `user` ADD COLUMN kakao_alarm BIT(1) NOT NULL DEFAULT b'0';

CREATE TABLE kakao_notify_consent (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT        NOT NULL,
    encrypted_refresh_token VARCHAR(1024) NOT NULL,
    scope                   VARCHAR(255)  NOT NULL,
    consented_at            DATETIME(6)   NOT NULL,
    -- 유저가 카카오에서 동의를 해지했거나 토큰이 더는 유효하지 않으면 채워진다. 채워지면 발송 대상에서 빠진다.
    revoked_at              DATETIME(6)   NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_kakao_notify_consent_user (user_id),
    CONSTRAINT fk_kakao_notify_consent_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
