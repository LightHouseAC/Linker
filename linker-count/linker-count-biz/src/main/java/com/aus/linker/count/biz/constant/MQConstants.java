package com.aus.linker.count.biz.constant;

public interface MQConstants {

    /**
     * Topic: 关注数计数
     */
    String TOPIC_COUNT_FOLLOWING = "CountFollowingTopic";

    /**
     * Topic: 粉丝数计数
     */
    String TOPIC_COUNT_FANS = "CountFansTopic";

    /**
     * Topic: 粉丝数落库
     */
    String TOPIC_COUNT_FANS_2_DB = "CountFans2DBTopic";

    /**
     * Topic: 关注数落库
     */
    String TOPIC_COUNT_FOLLOWING_2_DB = "CountFollowing2DBTopic";

    /**
     * Topic: 笔记点赞数计数
     */
    String TOPIC_COUNT_NOTE_LIKE = "CountNoteLikeTopic";

    /**
     * Topic: 笔记点赞数落库
     */
    String TOPIC_COUNT_NOTE_LIKE_2_DB = "CountNoteLike2DBTopic";

}
