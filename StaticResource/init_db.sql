CREATE TABLE `t_user` (
                          `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                          `linker_id` varchar(32) NOT NULL COMMENT 'linker号(唯一凭证)',
                          `password` varchar(64) DEFAULT NULL COMMENT '密码',
                          `nickname` varchar(24) NOT NULL COMMENT '昵称',
                          `avatar` varchar(120) DEFAULT NULL COMMENT '头像',
                          `birthday` date DEFAULT NULL COMMENT '生日',
                          `background_img` varchar(120) DEFAULT NULL COMMENT '背景图',
                          `phone` varchar(11) NOT NULL COMMENT '手机号',
                          `sex` tinyint DEFAULT '0' COMMENT '性别(0：女 1：男)',
                          `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0：启用 1：禁用)',
                          `introduction` varchar(100) DEFAULT NULL COMMENT '个人简介',
                          `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                          `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
                          PRIMARY KEY (`id`) USING BTREE,
                          UNIQUE KEY `uk_linker_id` (`linker_id`),
                          UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `t_role` (
                          `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                          `role_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名',
                          `role_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色唯一标识',
                          `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0：启用 1：禁用)',
                          `sort` int unsigned NOT NULL DEFAULT 0 COMMENT '管理系统中的显示顺序',
                          `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
                          `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后一次更新时间',
                          `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
                          PRIMARY KEY (`id`) USING BTREE,
                          UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE `t_permission` (
                                `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `parent_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '父ID',
                                `name` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
                                `type` tinyint unsigned NOT NULL COMMENT '类型(1：目录 2：菜单 3：按钮)',
                                `menu_url` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '菜单路由',
                                `menu_icon` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '菜单图标',
                                `sort` int unsigned NOT NULL DEFAULT 0 COMMENT '管理系统中的显示顺序',
                                `permission_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限标识',
                                `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态(0：启用；1：禁用)',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
                                PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE `t_user_role_rel` (
                                   `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
                                   `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色表';

CREATE TABLE `t_role_permission_rel` (
                                         `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                         `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
                                         `permission_id` bigint unsigned NOT NULL COMMENT '权限ID',
                                         `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
                                         PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户权限表';

INSERT INTO t_permission (`id`, `parent_id`, `name`, `type`, `menu_url`, `menu_icon`, `sort`, `permission_key`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES (1, 0, '发布笔记', 3, '', '', 1, 'app:note:publish', 0, now(), now(), b'0');
INSERT INTO t_permission (`id`, `parent_id`, `name`, `type`, `menu_url`, `menu_icon`, `sort`, `permission_key`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES (2, 0, '发布评论', 3, '', '', 2, 'app:comment:publish', 0, now(), now(), b'0');

INSERT INTO t_role (`id`, `role_name`, `role_key`, `status`, `sort`, `remark`, `create_time`, `update_time`, `is_deleted`) VALUES (1, '普通用户', 'common_user', 0, 1, '', now(), now(), b'0');

INSERT INTO t_role_permission_rel (`id`, `role_id`, `permission_id`, `create_time`, `update_time`, `is_deleted`) VALUES (1, 1, 1, now(), now(), b'0');
INSERT INTO t_role_permission_rel (`id`, `role_id`, `permission_id`, `create_time`, `update_time`, `is_deleted`) VALUES (2, 1, 2, now(), now(), b'0');

# 笔记服务相关 DDL

# 频道表
CREATE TABLE `t_channel` (
                             `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                             `name` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '频道名称',
                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                             `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
                             PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道表';

# 话题表
CREATE TABLE `t_topic` (
                           `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                           `name` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '话题名称',
                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                           `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
                           PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题表';

# 频道-话题关联表
CREATE TABLE `t_channel_topic_rel` (
                                       `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                       `channel_id` bigint(11) unsigned NOT NULL COMMENT '频道ID',
                                       `topic_id` bigint(11) unsigned NOT NULL COMMENT '话题ID',
                                       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                       PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道-话题关联表';

# 笔记表
CREATE TABLE `t_note` (
                          `id` bigint(11) unsigned NOT NULL COMMENT '主键ID',
                          `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
                          `is_content_empty` bit(1) NOT NULL DEFAULT b'0' COMMENT '内容是否为空(0：不为空 1：空)',
                          `creator_id` bigint(11) unsigned NOT NULL COMMENT '发布者ID',
                          `topic_id` bigint(11) unsigned DEFAULT NULL COMMENT '话题ID',
                          `topic_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '话题名称',
                          `is_top` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否置顶(0：未置顶 1：置顶)',
                          `type` tinyint(2) DEFAULT '0' COMMENT '类型(0：图文 1：视频)',
                          `img_uris` varchar(660) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '笔记图片链接(逗号隔开)',
                          `video_uri` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频链接',
                          `visible` tinyint(2) DEFAULT '0' COMMENT '可见范围(0：公开,所有人可见 1：仅对自己可见)',
                          `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                          `status` tinyint(2) NOT NULL DEFAULT '0' COMMENT '状态(0：待审核 1：正常展示 2：被删除(逻辑删除) 3：被下架)',
                          `content_uuid` varchar(36) DEFAULT '' COMMENT '笔记内容UUID',
                          PRIMARY KEY (`id`) USING BTREE,
                          KEY `idx_creator_id` (`creator_id`),
                          KEY `idx_topic_id` (`topic_id`),
                          KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表';

CREATE TABLE `t_following` (
                               `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
                               `following_user_id` bigint unsigned NOT NULL COMMENT '关注的用户ID',
                               `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

ALTER TABLE t_following ADD UNIQUE uk_user_id_following_user_id(user_id, following_user_id);

CREATE TABLE `t_fans` (
                          `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                          `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
                          `fans_user_id` bigint unsigned NOT NULL COMMENT '粉丝的用户ID',
                          `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户粉丝表';

ALTER TABLE t_fans ADD UNIQUE uk_user_id_fans_user_id(user_id, fans_user_id);

CREATE TABLE `t_note_like` (
                               `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               `user_id` bigint(11) NOT NULL COMMENT '用户ID',
                               `note_id` bigint(11) NOT NULL COMMENT '笔记ID',
                               `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `status` tinyint(2) NOT NULL DEFAULT '0' COMMENT '点赞状态(0：取消点赞 1：点赞)',
                               PRIMARY KEY (`id`) USING BTREE,
                               UNIQUE KEY `uk_user_id_note_id` (`user_id`,`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记点赞表';

CREATE TABLE `t_note_collection` (
                                     `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `user_id` bigint(11) NOT NULL COMMENT '用户ID',
                                     `note_id` bigint(11) NOT NULL COMMENT '笔记ID',
                                     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `status` tinyint(2) NOT NULL DEFAULT '0' COMMENT '收藏状态(0：取消收藏 1：收藏)',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     UNIQUE KEY `uk_user_id_note_id` (`user_id`,`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记收藏表';

CREATE TABLE `t_note_count` (
                                `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `note_id` bigint unsigned NOT NULL COMMENT '笔记ID',
                                `like_total` bigint DEFAULT '0' COMMENT '获得点赞总数',
                                `collect_total` bigint DEFAULT '0' COMMENT '获得收藏总数',
                                `comment_total` bigint DEFAULT '0' COMMENT '被评论总数',
                                PRIMARY KEY (`id`) USING BTREE,
                                UNIQUE KEY `uk_note_id` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记计数表';