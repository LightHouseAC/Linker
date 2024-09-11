package com.aus.linker.kv;

import com.aus.framework.common.utils.JsonUtil;
import com.aus.linker.kv.domain.dataobject.NoteContentDO;
import com.aus.linker.kv.domain.repository.NoteContentRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
@Slf4j
class CassandraTests {

    @Resource
    private NoteContentRepository noteContentRepository;

    /**
     * 测试插入数据
     */
    @Test
    void testInsert() {
        NoteContentDO noteContent = NoteContentDO.builder()
                .id(UUID.randomUUID())
                .content("代码测试笔记内容插入")
                .build();

        noteContentRepository.save(noteContent);
    }

    /**
     * 测试查询数据
     */
    @Test
    void testSelect() {
        Optional<NoteContentDO> optional = noteContentRepository.findById(UUID.fromString("59bc7f29-29cf-4eb3-9f99-96dacc392174"));
        optional.ifPresent(noteContentDO -> log.info("查询结果：{}", JsonUtil.toJsonString(noteContentDO)));
    }

    /**
     * 测试删除数据
     */
    @Test
    void testDelete() {
        noteContentRepository.deleteById(UUID.fromString("59bc7f29-29cf-4eb3-9f99-96dacc392174"));
    }

}
