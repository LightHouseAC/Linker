package com.aus.linker.note.biz.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aus.linker.note.biz.domain.dataobject.NoteDO;
import com.aus.linker.note.biz.domain.service.NoteService;
import com.aus.linker.note.biz.domain.mapper.NoteDOMapper;
import org.springframework.stereotype.Service;

/**
* @author recww
* @description 针对表【t_note(笔记表)】的数据库操作Service实现
* @createDate 2024-09-22 18:28:16
*/
@Service
public class NoteServiceImpl extends ServiceImpl<NoteDOMapper, NoteDO>
    implements NoteService {

}




