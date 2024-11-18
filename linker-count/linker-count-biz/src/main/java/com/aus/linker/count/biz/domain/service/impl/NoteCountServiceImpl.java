package com.aus.linker.count.biz.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aus.linker.count.biz.domain.dataobject.NoteCountDO;
import com.aus.linker.count.biz.domain.service.NoteCountService;
import com.aus.linker.count.biz.domain.mapper.NoteCountDOMapper;
import org.springframework.stereotype.Service;

/**
* @author lance.yang
* @description 针对表【t_note_count(笔记计数表)】的数据库操作Service实现
* @createDate 2024-11-18 14:45:35
*/
@Service
public class NoteCountServiceImpl extends ServiceImpl<NoteCountDOMapper, NoteCountDO>
    implements NoteCountService {

}




