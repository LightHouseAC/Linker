package com.aus.linker.oss.strategy.impl;

import com.aliyun.oss.OSS;
import com.aus.linker.oss.config.AliyunOSSProperties;
import com.aus.linker.oss.strategy.FileStrategy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
public class AliyunFileStrategy implements FileStrategy {

    @Resource
    private AliyunOSSProperties aliyunOSSProperties;

    @Resource
    private OSS ossClient;

    private final String bucketName;

    public AliyunFileStrategy(String bucket) {
        this.bucketName = bucket;
    }

    @Override
    @SneakyThrows
    public String uploadFile(MultipartFile file) {
        log.info("## 上传文件至 阿里云OSS ...");

        // 判断文件是否是空
        if (file == null || file.getSize() == 0){
            log.error("==> 上传文件异常：文件大小为空 ...");
            throw new RuntimeException("文件大小不能为空");
        }

        // 文件的原始名称
        String originalFileName = file.getOriginalFilename();

        // 生成存储对象的名称（将 UUID 字符串中的 - 替换成空字符串）
        String key = UUID.randomUUID().toString().replace("-", "");
        // 获取文件的后缀，如 .jpg
        String suffix = originalFileName.substring(originalFileName.lastIndexOf("."));

        // 拼接上文件后缀，即为要存储的文件名
        String objectName = String.format("%s%s", key, suffix);

        log.info("==> 开始上传文件至阿里云 OSS, ObjectName: {}", objectName);

        // 上传文件至阿里云 OSS
        ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(file.getInputStream().readAllBytes()));

        // 返回文件的访问链接
        String url = String.format("https://%s.%s/%s", bucketName, aliyunOSSProperties.getEndpoint(), objectName);
        log.info("==> 上传文件至阿里云 OSS 成功，访问路径: {}", url);
        return url;
    }
}