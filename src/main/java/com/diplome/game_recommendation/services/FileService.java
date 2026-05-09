package com.diplome.game_recommendation.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;


@Service
public class FileService {

    private final MinioClient minioClient;
    @Value("${minio.bucket}") private String bucketName;

    public FileService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String uploadAvatar(MultipartFile file, Long userId) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            String fileName = "avatars/" + userId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки файла в MinIO: " + e.getMessage());
        }
    }
}