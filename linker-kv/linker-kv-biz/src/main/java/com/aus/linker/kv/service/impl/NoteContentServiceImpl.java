package com.aus.linker.kv.service.impl;

import com.aus.framework.common.exception.BizException;
import com.aus.framework.common.response.Response;
import com.aus.linker.kv.domain.dataobject.NoteContentDO;
import com.aus.linker.kv.domain.repository.NoteContentRepository;
import com.aus.linker.kv.dto.req.AddNoteContentReqDTO;
import com.aus.linker.kv.dto.req.DeleteNoteContentReqDTO;
import com.aus.linker.kv.dto.req.FindNoteContentReqDTO;
import com.aus.linker.kv.dto.resp.FindNoteContentRespDTO;
import com.aus.linker.kv.enums.ResponseCodeEnum;
import com.aus.linker.kv.service.NoteContentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NoteContentServiceImpl implements NoteContentService {

    @Resource
    private NoteContentRepository noteContentRepository;

    @Override
    public Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO) {
        // 笔记内容 UUID
        String uuid = addNoteContentReqDTO.getUuid();
        // 笔记内容
        String content = addNoteContentReqDTO.getContent();

        // 构建Cassandra库 DO 实现类
        NoteContentDO noteContent = NoteContentDO.builder()
                .id(UUID.fromString(uuid))
                .content(content)
                .build();

        // 插入数据
        noteContentRepository.save(noteContent);
        return Response.success();
    }

    @Override
    public Response<FindNoteContentRespDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO) {
        // 笔记内容 UUID
        String uuid = findNoteContentReqDTO.getUuid();
        // 根据笔记 ID 查询笔记内容
        Optional<NoteContentDO> optional = noteContentRepository.findById(UUID.fromString(uuid));

        // 若笔记内容不存在
        if (!optional.isPresent()) {
            throw new BizException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
        }

        NoteContentDO noteContentDO = optional.get();
        // 构建反参 DTO
        FindNoteContentRespDTO findNoteContentRespDTO = FindNoteContentRespDTO.builder()
                .uuid(noteContentDO.getId())
                .content(noteContentDO.getContent())
                .build();

        return Response.success(findNoteContentRespDTO);
    }

    @Override
    public Response<?> deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO) {
        // 笔记内容 UUID
        String uuid = deleteNoteContentReqDTO.getUuid();
        // 删除笔记内容
        noteContentRepository.deleteById(UUID.fromString(uuid));

        return Response.success();
    }

}
