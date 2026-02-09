package com.movie.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.movie.config.MinioConfig;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;

@Service
public class StorageService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 上传文件 (自动处理权限)
     */
    public String upload(MultipartFile file) {
        String bucketName = minioConfig.getBucketName();
        try {
            // 1. 检查存储桶，不存在则创建
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                
                // ================== 核心黑科技：代码自动设置公开权限 ==================
                // 这段 JSON 是 AWS S3 标准的权限策略，允许任何人(Principal: *)读取(Action: s3:GetObject)
                String policyJson = """
                                    {
                                    "Version": "2012-10-17",
                                    "Statement": [
                                        {
                                        "Effect": "Allow",
                                        "Principal": {
                                            "AWS": [
                                            "*"
                                            ]
                                        },
                                        "Action": [
                                            "s3:GetObject"
                                        ],
                                        "Resource": [
                                            "arn:aws:s3:::""" + bucketName + "/*\"\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
                
                // 执行设置策略
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policyJson)
                                .build()
                );
                // ===================================================================
            }

            // 2. 生成文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || StrUtil.isBlank(originalFilename)) {
                throw new RuntimeException("文件名不能为空");
            }
            String suffix = ".jpg";
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex != -1) {
                suffix = originalFilename.substring(lastDotIndex);
            }
            String fileName = IdUtil.simpleUUID() + suffix;

            // 3. 上传
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 4. 返回 URL
            return minioConfig.getEndpoint() + "/" + bucketName + "/" + fileName;

        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidResponseException | ServerException | XmlParserException | IOException | RuntimeException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException("上传失败: " + e.getMessage());
        }
    }
}