package com.aus.linker.kv.api;

import com.aus.framework.common.response.Response;
import com.aus.linker.kv.constant.ApiConstants;
import com.aus.linker.kv.dto.req.AddNoteContentReqDTO;
import com.aus.linker.kv.dto.req.DeleteNoteContentReqDTO;
import com.aus.linker.kv.dto.req.FindNoteContentReqDTO;
import com.aus.linker.kv.dto.resp.FindNoteContentRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface KeyValueFeignApi {

    String PREFIX = "/kv";

    @PostMapping(value = PREFIX + "/note/content/add")
    Response<?> addNoteContent(@RequestBody AddNoteContentReqDTO addNoteContentReqDTO);

    @PostMapping(value = PREFIX + "/note/content/delete")
    Response<?> deleteNoteContent(@RequestBody DeleteNoteContentReqDTO deleteNoteContentReqDTO);

    @PostMapping(value = PREFIX + "/note/content/find")
    Response<FindNoteContentRespDTO> findNoteContent(@RequestBody FindNoteContentReqDTO findNoteContentReqDTO);

}
