-- ============================================================
-- H2 (MODE=MySQL) schema for integration tests.
-- Adapted from doc/detailed-design.md Appendix A:
--   * MySQL-specific clauses removed: ENGINE, COLLATE, COMMENT, ON UPDATE CURRENT_TIMESTAMP
--   * DATETIME -> TIMESTAMP, TINYINT(1) -> TINYINT
--   * Index / unique-constraint names are globally unique in H2 (unlike MySQL where they
--     are per-table), so colliding names are prefixed with their table name here.
--   * browse_history UPSERT is annotation-SQL only (not exercised by the H2 schema).
-- ============================================================

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(50)  NOT NULL,
  `password`   VARCHAR(100) NOT NULL,
  `nickname`   VARCHAR(50)  DEFAULT NULL,
  `avatar`     VARCHAR(255) DEFAULT NULL,
  `role`       VARCHAR(20)  NOT NULL DEFAULT 'user',
  `status`     TINYINT      NOT NULL DEFAULT 1,
  `create_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at`  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_username` UNIQUE (`username`)
);
CREATE INDEX `idx_user_role` ON `user` (`role`);

DROP TABLE IF EXISTS `scenic_spot`;
CREATE TABLE `scenic_spot` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `name`           VARCHAR(100)  NOT NULL,
  `city`           VARCHAR(50)   NOT NULL,
  `address`        VARCHAR(255)  DEFAULT NULL,
  `category`       VARCHAR(50)   DEFAULT NULL,
  `price`          DECIMAL(10,2) DEFAULT 0.00,
  `open_time`      VARCHAR(100)  DEFAULT NULL,
  `description`    TEXT,
  `tags`           VARCHAR(255)  DEFAULT NULL,
  `images`         VARCHAR(1000) DEFAULT NULL,
  `avg_rating`     DECIMAL(3,2)  DEFAULT 0.00,
  `favorite_count` INT           DEFAULT 0,
  `view_count`     INT           DEFAULT 0,
  `create_at`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at`      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`        TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);
CREATE INDEX `idx_scenic_city` ON `scenic_spot` (`city`);
CREATE INDEX `idx_scenic_category` ON `scenic_spot` (`category`);
CREATE INDEX `idx_scenic_name` ON `scenic_spot` (`name`);

DROP TABLE IF EXISTS `trip`;
CREATE TABLE `trip` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL,
  `title`       VARCHAR(100) NOT NULL,
  `city`        VARCHAR(50)  NOT NULL,
  `days`        INT          NOT NULL,
  `budget`      VARCHAR(50)  DEFAULT NULL,
  `spend_mode`  VARCHAR(20)  DEFAULT NULL,
  `preferences` VARCHAR(255) DEFAULT NULL,
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `create_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);
CREATE INDEX `idx_trip_user_create` ON `trip` (`user_id`, `create_at`);
CREATE INDEX `idx_trip_city` ON `trip` (`city`);

DROP TABLE IF EXISTS `trip_day`;
CREATE TABLE `trip_day` (
  `id`        BIGINT       NOT NULL AUTO_INCREMENT,
  `trip_id`   BIGINT       NOT NULL,
  `day_no`    INT          NOT NULL,
  `theme`     VARCHAR(100) DEFAULT NULL,
  `lunch`     VARCHAR(255) DEFAULT NULL,
  `dinner`    VARCHAR(255) DEFAULT NULL,
  `tip`       VARCHAR(500) DEFAULT NULL,
  `create_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);
CREATE INDEX `idx_trip_day` ON `trip_day` (`trip_id`, `day_no`);

DROP TABLE IF EXISTS `trip_spot`;
CREATE TABLE `trip_spot` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `trip_id`      BIGINT       NOT NULL,
  `scenic_id`    BIGINT       DEFAULT NULL,
  `day_no`       INT          NOT NULL,
  `spot_name`    VARCHAR(100) NOT NULL,
  `visit_order`  INT          NOT NULL,
  `stay_minutes` INT          DEFAULT 60,
  `tip`          VARCHAR(500) DEFAULT NULL,
  `create_at`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);
CREATE INDEX `idx_trip_spot_order` ON `trip_spot` (`trip_id`, `day_no`, `visit_order`);

DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
  `id`        BIGINT    NOT NULL AUTO_INCREMENT,
  `user_id`   BIGINT    NOT NULL,
  `scenic_id` BIGINT    NOT NULL,
  `create_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_favorite_user_scenic` UNIQUE (`user_id`, `scenic_id`)
);
CREATE INDEX `idx_favorite_user_create` ON `favorite` (`user_id`, `create_at`);

DROP TABLE IF EXISTS `browse_history`;
CREATE TABLE `browse_history` (
  `id`          BIGINT    NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT    NOT NULL,
  `scenic_id`   BIGINT    NOT NULL,
  `browse_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_browse_user_scenic` UNIQUE (`user_id`, `scenic_id`)
);
CREATE INDEX `idx_browse_user_time` ON `browse_history` (`user_id`, `browse_time`);

DROP TABLE IF EXISTS `ai_chat`;
CREATE TABLE `ai_chat` (
  `id`        BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`   BIGINT      NOT NULL,
  `trip_id`   BIGINT      DEFAULT NULL,
  `role`      VARCHAR(10) NOT NULL,
  `content`   TEXT        NOT NULL,
  `create_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);
CREATE INDEX `idx_chat_user_create` ON `ai_chat` (`user_id`, `create_at`);
CREATE INDEX `idx_chat_trip` ON `ai_chat` (`trip_id`);
