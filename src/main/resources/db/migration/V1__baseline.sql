-- V1: 운영 DB(cabi) 스키마 베이스라인.
--
-- 운영 DB 의 `mysqldump --no-data` 를 scripts/db/sanitize_baseline.py 로 정제한 것이다.
-- 이 파일은 새 DB 를 처음부터 만들 때만 실행된다. 운영 DB 는 사람이 한 번
-- `flyway baseline -baselineVersion=1` 로 이 버전을 이미 적용됨으로 표시하므로 실행되지 않는다.
-- (baselineOnMigrate 는 사용하지 않는다. docs/db/FLYWAY_BASELINE.md 참고)
--
-- 주의: 이 파일에는 CREATE TABLE 만 둔다. DROP/DELETE/TRUNCATE/INSERT 를 넣지 말 것
-- (FlywayBaselineV1GuardTest 가 막는다).

-- banned_user
CREATE TABLE `banned_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `banned_at` datetime(6) NOT NULL,
  `intra_id` varchar(32) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK46xman4tp1klw08kkks2b5um5` (`intra_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- cabinet
CREATE TABLE `cabinet` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `grid_col` int DEFAULT NULL,
  `floor` int DEFAULT NULL,
  `lent_type` enum('LAPISCINE','PRIVATE') NOT NULL,
  `max_user` int NOT NULL,
  `grid_row` int DEFAULT NULL,
  `section` varchar(255) DEFAULT NULL,
  `status` enum('AVAILABLE','BROKEN','DISABLED','FULL','OVERDUE','PENDING') NOT NULL,
  `status_note` varchar(64) DEFAULT NULL,
  `visible_num` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_cabinet_visible_num` (`visible_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- item
CREATE TABLE `item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `price` bigint NOT NULL,
  `type` enum('EXTENSION','LENT','PENALTY_EXEMPTION','SWAP') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `blackholed_at` datetime(6) DEFAULT NULL,
  `coin` bigint NOT NULL DEFAULT '0',
  `deleted_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `email_alarm` bit(1) DEFAULT b'0',
  `monthly_logtime` int NOT NULL DEFAULT '0',
  `name` varchar(32) NOT NULL,
  `penalty_days` int NOT NULL DEFAULT '0',
  `push_alarm` bit(1) DEFAULT b'0',
  `role` enum('ADMIN','MASTER','USER') NOT NULL DEFAULT 'USER',
  `slack_alarm` bit(1) DEFAULT b'0',
  `version` bigint NOT NULL DEFAULT '1',
  `is_pisciner` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgj2fy3dcix7ph7k8684gka40c` (`name`),
  UNIQUE KEY `UKob8kqyqqgmefl0aco34akdtpe` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- watermelon
CREATE TABLE `watermelon` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `current_level` int NOT NULL,
  `dangerous_fertilizer_count` int NOT NULL,
  `destroy_protection_count` int NOT NULL,
  `drop_protection_count` int NOT NULL,
  `highest_level` int NOT NULL,
  `highest_level_achieved_at` datetime(6) NOT NULL,
  `premium_fertilizer_count` int NOT NULL,
  `total_attempts` int NOT NULL,
  `total_destroys` int NOT NULL,
  `total_drops` int NOT NULL,
  `total_maintains` int NOT NULL,
  `total_successes` int NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKju0seaahg22d1j1lq02f4ilq1` (`user_id`),
  KEY `idx_watermelon_ranking` (`highest_level` DESC,`highest_level_achieved_at`,`total_attempts`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- watermelon_event_log
CREATE TABLE `watermelon_event_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `after_level` int NOT NULL,
  `before_level` int NOT NULL,
  `cost_seeds` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `final_outcome` varchar(20) NOT NULL,
  `raw_outcome` varchar(20) NOT NULL,
  `used_dangerous_fertilizer` bit(1) NOT NULL,
  `used_destroy_protection` bit(1) NOT NULL,
  `used_drop_protection` bit(1) NOT NULL,
  `used_premium_fertilizer` bit(1) NOT NULL,
  `user_id` bigint NOT NULL,
  `user_name` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_wm_log_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- attendance
CREATE TABLE `attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attendance_date` date NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6y2t3r1ig8a9ccqwbrs6wk4l8` (`user_id`,`attendance_date`),
  CONSTRAINT `FK46cuxphi3uh5quom51s6i2q8x` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- calendar_event
CREATE TABLE `calendar_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `event_date` date NOT NULL,
  `title` varchar(100) NOT NULL,
  `announcer_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKcdv8sb1lgouddyefsbldv50fj` (`announcer_id`),
  CONSTRAINT `FKcdv8sb1lgouddyefsbldv50fj` FOREIGN KEY (`announcer_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- coin_history
CREATE TABLE `coin_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `type` enum('ADMIN_GRANT','ADMIN_REVOKE','ATTENDANCE','ITEM_PURCHASE','WATERMELON') NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkyswxwtp5lafq2cul9v84etdy` (`user_id`),
  CONSTRAINT `FKkyswxwtp5lafq2cul9v84etdy` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- item_history
CREATE TABLE `item_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `purchase_at` datetime(6) NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `item_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKid04cy8p2t4mrcenqipyu7l4n` (`item_id`),
  KEY `FKhpaemxx3ni5ee2bgdlxxku8ok` (`user_id`),
  CONSTRAINT `FKhpaemxx3ni5ee2bgdlxxku8ok` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKid04cy8p2t4mrcenqipyu7l4n` FOREIGN KEY (`item_id`) REFERENCES `item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- lent_history
CREATE TABLE `lent_history` (
  `lent_history_id` bigint NOT NULL AUTO_INCREMENT,
  `ended_at` datetime(6) DEFAULT NULL,
  `expired_at` datetime(6) NOT NULL,
  `is_auto_extension` bit(1) NOT NULL,
  `photo_url` varchar(255) DEFAULT NULL,
  `return_memo` varchar(255) DEFAULT NULL,
  `started_at` datetime(6) NOT NULL,
  `cabinet_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`lent_history_id`),
  KEY `idx_lent_user_id` (`user_id`),
  KEY `idx_lent_cabinet_id` (`cabinet_id`),
  KEY `idx_lent_ended_at` (`ended_at`),
  CONSTRAINT `FK65rj7u9eih0x63rpeyoq5gp2h` FOREIGN KEY (`cabinet_id`) REFERENCES `cabinet` (`id`),
  CONSTRAINT `FKp4gd80p8ruvkxqvxhqpy37wvu` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- oauth_link
CREATE TABLE `oauth_link` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `linked_at` datetime(6) NOT NULL,
  `provider` varchar(20) NOT NULL,
  `provider_email` varchar(100) DEFAULT NULL,
  `provider_id` varchar(100) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dvyurxmkk3axw3eb9h757m16` (`provider`,`provider_id`),
  KEY `FKelroqlxmrw4uyqhr0ods8t715` (`user_id`),
  CONSTRAINT `FKelroqlxmrw4uyqhr0ods8t715` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
