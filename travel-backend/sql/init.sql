-- ============================================================
-- AI 旅游攻略生成与问答助手 — 数据库初始化脚本
-- MySQL 8.0 / InnoDB / utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `ai_travel`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `ai_travel`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------- user ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`         bigint       NOT NULL AUTO_INCREMENT,
  `username`   varchar(50)  NOT NULL                COMMENT '用户名',
  `password`   varchar(100) NOT NULL                COMMENT '密码(BCrypt)',
  `nickname`   varchar(50)  DEFAULT NULL            COMMENT '昵称',
  `avatar`     varchar(255) DEFAULT NULL            COMMENT '头像URL',
  `role`       varchar(20)  NOT NULL DEFAULT 'user' COMMENT 'user/admin',
  `status`     tinyint(1)   NOT NULL DEFAULT 1      COMMENT '0=禁用 1=正常',
  `create_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    tinyint(1)   NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ------------------------- scenic_spot ------------------------
DROP TABLE IF EXISTS `scenic_spot`;
CREATE TABLE `scenic_spot` (
  `id`             bigint        NOT NULL AUTO_INCREMENT,
  `name`           varchar(100)  NOT NULL              COMMENT '景点名称',
  `city`           varchar(50)   NOT NULL              COMMENT '所在城市',
  `address`        varchar(255)  DEFAULT NULL          COMMENT '详细地址',
  `category`       varchar(50)   DEFAULT NULL          COMMENT '自然/人文/主题乐园/其他',
  `price`          decimal(10,2) DEFAULT 0.00          COMMENT '门票价格(0=免费)',
  `open_time`      varchar(100)  DEFAULT NULL          COMMENT '开放时间',
  `description`    text                                COMMENT '景点介绍',
  `tags`           varchar(255)  DEFAULT NULL          COMMENT '标签,逗号分隔',
  `images`         varchar(1000) DEFAULT NULL          COMMENT '图片URL,逗号分隔',
  `avg_rating`     decimal(3,2)  DEFAULT 0.00          COMMENT '平均评分',
  `favorite_count` int           DEFAULT 0             COMMENT '收藏数(收藏/取消时同步维护)',
  `view_count`     int           DEFAULT 0             COMMENT '浏览量',
  `create_at`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        tinyint(1)    NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_city` (`city`),
  KEY `idx_category` (`category`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='景点信息表';

-- ---------------------------- trip ----------------------------
DROP TABLE IF EXISTS `trip`;
CREATE TABLE `trip` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `user_id`     bigint       NOT NULL,
  `title`       varchar(100) NOT NULL              COMMENT '行程标题',
  `city`        varchar(50)  NOT NULL              COMMENT '目的地城市',
  `days`        int          NOT NULL              COMMENT '旅行天数',
  `budget`      varchar(50)  DEFAULT NULL          COMMENT '预算范围',
  `spend_mode`  varchar(20)  DEFAULT NULL          COMMENT '经济/舒适/高端',
  `preferences` varchar(255) DEFAULT NULL          COMMENT '兴趣偏好,逗号分隔',
  `status`      tinyint(1)   NOT NULL DEFAULT 1    COMMENT '0=草稿 1=已完成',
  `create_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     tinyint(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_create` (`user_id`, `create_at`),
  KEY `idx_city` (`city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='行程主表';

-- --------------------------- trip_day -------------------------
DROP TABLE IF EXISTS `trip_day`;
CREATE TABLE `trip_day` (
  `id`        bigint       NOT NULL AUTO_INCREMENT,
  `trip_id`   bigint       NOT NULL,
  `day_no`    int          NOT NULL              COMMENT '第几天',
  `theme`     varchar(100) DEFAULT NULL          COMMENT '当日主题',
  `lunch`     varchar(255) DEFAULT NULL          COMMENT '午餐推荐',
  `dinner`    varchar(255) DEFAULT NULL          COMMENT '晚餐推荐',
  `tip`       varchar(500) DEFAULT NULL          COMMENT '当日提示',
  `create_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_trip_day` (`trip_id`, `day_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日行程表';

-- -------------------------- trip_spot -------------------------
DROP TABLE IF EXISTS `trip_spot`;
CREATE TABLE `trip_spot` (
  `id`           bigint       NOT NULL AUTO_INCREMENT,
  `trip_id`      bigint       NOT NULL,
  `scenic_id`    bigint       DEFAULT NULL          COMMENT '景点ID(AI生成的可能无对应景点)',
  `day_no`       int          NOT NULL              COMMENT '第几天',
  `spot_name`    varchar(100) NOT NULL              COMMENT '景点名称',
  `visit_order`  int          NOT NULL              COMMENT '访问顺序',
  `stay_minutes` int          DEFAULT 60            COMMENT '建议停留(分钟)',
  `tip`          varchar(500) DEFAULT NULL          COMMENT '游玩提示',
  `create_at`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_trip_day_order` (`trip_id`, `day_no`, `visit_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='行程-景点关联表';

-- -------------------------- favorite --------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
  `id`        bigint   NOT NULL AUTO_INCREMENT,
  `user_id`   bigint   NOT NULL,
  `scenic_id` bigint   NOT NULL,
  `create_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_scenic` (`user_id`, `scenic_id`),
  KEY `idx_user_create` (`user_id`, `create_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='收藏表';

-- ----------------------- browse_history -----------------------
DROP TABLE IF EXISTS `browse_history`;
CREATE TABLE `browse_history` (
  `id`          bigint   NOT NULL AUTO_INCREMENT,
  `user_id`     bigint   NOT NULL,
  `scenic_id`   bigint   NOT NULL,
  `browse_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_scenic` (`user_id`, `scenic_id`),
  KEY `idx_user_time` (`user_id`, `browse_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='浏览记录表';

-- --------------------------- ai_chat --------------------------
DROP TABLE IF EXISTS `ai_chat`;
CREATE TABLE `ai_chat` (
  `id`        bigint      NOT NULL AUTO_INCREMENT,
  `user_id`   bigint      NOT NULL,
  `trip_id`   bigint      DEFAULT NULL            COMMENT '关联行程ID(可选)',
  `role`      varchar(10) NOT NULL                COMMENT 'user/assistant',
  `content`   text        NOT NULL                COMMENT '对话内容',
  `create_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_create` (`user_id`, `create_at`),
  KEY `idx_trip` (`trip_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI对话记录表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 种子数据
-- ============================================================

-- 管理员：密码 admin123；演示用户：密码 user123（上线前必须修改）
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`) VALUES
('admin', '$2b$10$mKgOn1CboSJF4qR1.Bv1neI6nsg2y5ql41L553In7V3diiWHt448C', '系统管理员', 'admin', 1),
('demo',  '$2b$10$yWe7meDh77DJe6Tx9/LCYefs/Td3Cc59tDnbvcxn33IlU1zQsK8ti', '演示用户',   'user',  1);

INSERT INTO `scenic_spot`
(`name`, `city`, `address`, `category`, `price`, `open_time`, `description`, `tags`, `images`, `avg_rating`, `favorite_count`, `view_count`) VALUES
('故宫博物院',       '北京', '北京市东城区景山前街4号',   '人文',     60.00,  '08:30-17:00（周一闭馆）', '明清两代皇家宫殿，世界现存规模最大的木质结构古建筑群。', '历史,建筑,博物馆,亲子', 'https://cdn.example.com/scenic/gugong-1.jpg,https://cdn.example.com/scenic/gugong-2.jpg', 4.90, 320, 8800),
('八达岭长城',       '北京', '北京市延庆区八达岭镇',       '人文',     40.00,  '06:30-16:30',            '明长城的重要关隘，以险峻雄伟著称。',                     '历史,徒步,世界遗产',   'https://cdn.example.com/scenic/badaling-1.jpg', 4.80, 260, 7200),
('颐和园',           '北京', '北京市海淀区新建宫门路19号', '人文',     30.00,  '06:30-18:00',            '清代皇家园林，以昆明湖、万寿山为主体。',                 '历史,园林,摄影',       'https://cdn.example.com/scenic/yiheyuan-1.jpg', 4.70, 180, 5100),
('南锣鼓巷',         '北京', '北京市东城区南锣鼓巷',       '人文',      0.00,  '全天',                   '老北京胡同风貌街区，聚集特色小吃与文创小店。',           '美食,人文,免费,夜游',  'https://cdn.example.com/scenic/nanluoguxiang-1.jpg', 4.30, 150, 6300),
('北京环球度假区',   '北京', '北京市通州区京哈高速与东六环交汇处', '主题乐园', 528.00, '09:00-20:00',        '包含哈利·波特、变形金刚等主题园区的大型度假区。',        '主题乐园,亲子,夜游',   'https://cdn.example.com/scenic/universal-1.jpg', 4.60, 410, 9600),

('外滩',             '上海', '上海市黄浦区中山东一路',     '人文',      0.00,  '全天',                   '黄浦江西岸的万国建筑博览群，夜景著名。',                 '建筑,免费,夜游,摄影',  'https://cdn.example.com/scenic/waitan-1.jpg', 4.70, 300, 8100),
('上海迪士尼乐园',   '上海', '上海市浦东新区申迪西路753号','主题乐园', 475.00, '08:30-20:30',            '中国大陆首座迪士尼主题乐园。',                           '主题乐园,亲子,夜游',   'https://cdn.example.com/scenic/disney-1.jpg', 4.70, 520, 9900),
('豫园',             '上海', '上海市黄浦区安仁街218号',    '人文',     40.00,  '08:45-16:45',            '明代江南古典园林，毗邻城隍庙小吃街。',                   '园林,美食,历史',       'https://cdn.example.com/scenic/yuyuan-1.jpg', 4.40, 190, 4200),

('西湖',             '杭州', '浙江省杭州市西湖区龙井路1号','自然',      0.00,  '全天',                   '世界文化遗产，以十景闻名，可环湖骑行或游船。',           '自然,免费,骑行,摄影',  'https://cdn.example.com/scenic/xihu-1.jpg', 4.80, 460, 9300),
('灵隐寺',           '杭州', '浙江省杭州市西湖区法云弄1号','人文',     75.00,  '06:30-18:00',            '江南著名古刹，飞来峰造像与佛教文化胜地。',               '历史,宗教,自然',       'https://cdn.example.com/scenic/lingyin-1.jpg', 4.60, 210, 4700),
('千岛湖',           '杭州', '浙江省杭州市淳安县千岛湖镇', '自然',    130.00,  '08:00-17:00',            '人工湖景观，岛屿星罗棋布，适合游船与水上活动。',         '自然,亲子,水上活动',   'https://cdn.example.com/scenic/qiandaohu-1.jpg', 4.50, 140, 3800),

('成都大熊猫繁育研究基地', '成都', '四川省成都市成华区熊猫大道1375号', '自然', 55.00, '07:30-18:00',    '近距离观赏大熊猫的科研与繁育基地，清晨最活跃。',         '自然,亲子,动物',       'https://cdn.example.com/scenic/panda-1.jpg', 4.80, 380, 8600),
('宽窄巷子',         '成都', '四川省成都市青羊区金河路口', '人文',      0.00,  '全天',                   '由宽巷子、窄巷子、井巷子组成的清代古街巷。',             '美食,人文,免费,夜游',  'https://cdn.example.com/scenic/kuanzhai-1.jpg', 4.40, 280, 7100),
('都江堰',           '成都', '四川省成都市都江堰市公园路', '人文',     80.00,  '08:00-18:00',            '战国时期修建的大型水利工程，至今仍在灌溉成都平原。',     '历史,水利,世界遗产',   'https://cdn.example.com/scenic/dujiangyan-1.jpg', 4.70, 200, 4400);
