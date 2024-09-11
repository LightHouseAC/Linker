package com.aus.linker.oss.strategy;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件策略接口
 */
public interface FileStrategy {

    /**
     * 文件上传
     * @param file
     * @return
     */
    String uploadFile(MultipartFile file);

}
