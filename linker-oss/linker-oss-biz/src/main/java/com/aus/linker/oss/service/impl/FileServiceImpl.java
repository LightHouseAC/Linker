package com.aus.linker.oss.service.impl;

import com.aus.framework.common.response.Response;
import com.aus.linker.oss.service.FileService;
import com.aus.linker.oss.strategy.FileStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Resource
    private FileStrategy fileStrategy;

    @Override
    public Response<?> uploadFile(MultipartFile file) {
        // 上传文件
        String url = fileStrategy.uploadFile(file);

        return Response.success(url);
    }
}
