INSERT INTO `linker`.`t_channel` (`name`, `create_time`, `update_time`, `is_deleted`) VALUES ('美食', now(), now(), 0);
INSERT INTO `linker`.`t_channel` (`name`, `create_time`, `update_time`, `is_deleted`) VALUES ('娱乐', now(), now(), 0);

INSERT INTO `linker`.`t_topic` (`name`, `create_time`, `update_time`, `is_deleted`) VALUES ('高分美剧推荐', now(), now(), 0);
INSERT INTO `linker`.`t_topic` (`name`, `create_time`, `update_time`, `is_deleted`) VALUES ('下饭综艺推荐', now(), now(), 0);

INSERT INTO `linker`.`t_channel_topic_rel` (`channel_id`, `topic_id`, `create_time`, `update_time`) VALUES (2, 1, now(), now());
INSERT INTO `linker`.`t_channel_topic_rel` (`channel_id`, `topic_id`, `create_time`, `update_time`) VALUES (2, 2, now(), now());
