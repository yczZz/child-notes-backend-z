package com.ycz.childnotesbackend.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.ycz.childnotesbackend.config.OssProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class OssService {

    private final OssProperties ossProperties;

    public OssService(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    public String upload(MultipartFile file) throws IOException {
        if (!StringUtils.hasText(ossProperties.getAccessKeyId())) {
            throw new IllegalStateException("OSS not configured");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = getExtension(file.getOriginalFilename());
        String key = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        OSS ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret());

        try {
            ossClient.putObject(ossProperties.getBucketName(), key, file.getInputStream());
        } finally {
            ossClient.shutdown();
        }

        if (StringUtils.hasText(ossProperties.getBaseUrl())) {
            return ossProperties.getBaseUrl().replaceAll("/$", "") + "/" + key;
        }
        return "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/" + key;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
