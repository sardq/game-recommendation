package com.diplome.game_recommendation.services;


import io.minio.*;
import io.minio.errors.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private FileService fileService;

    private static final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "bucketName", BUCKET_NAME);
    }

    @Test
    void uploadAvatar_Success_WhenBucketExists() throws Exception {
         
        Long userId = 123L;
        String originalFilename = "avatar.jpg";
        byte[] content = "test image content".getBytes();
        
        MultipartFile file = new MockMultipartFile(
            "file", 
            originalFilename, 
            "image/jpeg", 
            content
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        
        String result = fileService.uploadAvatar(file, userId);

         
        assertNotNull(result);
        assertTrue(result.startsWith("avatars/" + userId + "_"));
        assertTrue(result.endsWith("_" + originalFilename));

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadAvatar_Success_WhenBucketDoesNotExist() throws Exception {
         
        Long userId = 456L;
        String originalFilename = "profile.png";
        byte[] content = "test image content".getBytes();
        
        MultipartFile file = new MockMultipartFile(
            "file", 
            originalFilename, 
            "image/png", 
            content
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        
        String result = fileService.uploadAvatar(file, userId);

         
        assertNotNull(result);
        assertTrue(result.startsWith("avatars/" + userId + "_"));

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadAvatar_ShouldUseCorrectFileName() throws Exception {
         
        Long userId = 789L;
        String originalFilename = "game_avatar.gif";
        byte[] content = "test gif content".getBytes();
        
        MultipartFile file = new MockMultipartFile(
            "file", 
            originalFilename, 
            "image/gif", 
            content
        );

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        
        String result = fileService.uploadAvatar(file, userId);

         
        verify(minioClient).putObject(captor.capture());
        PutObjectArgs capturedArgs = captor.getValue();
        
        assertTrue(capturedArgs.object().startsWith("avatars/" + userId + "_"));
        assertTrue(capturedArgs.object().endsWith(originalFilename));
        assertEquals("image/gif", capturedArgs.contentType());
    }

    @Test
    void uploadAvatar_ShouldHandleFileWithSpacesInName() throws Exception {
         
        Long userId = 999L;
        String originalFilename = "my avatar photo.jpg";
        byte[] content = "test content".getBytes();
        
        MultipartFile file = new MockMultipartFile(
            "file", 
            originalFilename, 
            "image/jpeg", 
            content
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        
        String result = fileService.uploadAvatar(file, userId);

         
        assertTrue(result.contains("my avatar photo.jpg"));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadAvatar_ShouldThrowException_WhenMinioFails() throws Exception {
         
        Long userId = 123L;
        MultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            "image/jpeg", 
            "content".getBytes()
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
            .thenThrow(new RuntimeException("MinIO connection failed"));

        RuntimeException exception = assertThrows(
            RuntimeException.class, 
            () -> fileService.uploadAvatar(file, userId)
        );

        assertTrue(exception.getMessage().contains("Ошибка загрузки файла в MinIO"));
        assertTrue(exception.getMessage().contains("MinIO connection failed"));
    }

    @Test
    void uploadAvatar_ShouldHandleLargeFile() throws Exception {
         
        Long userId = 111L;
        byte[] largeContent = new byte[10 * 1024 * 1024]; // 10 MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }
        
        MultipartFile file = new MockMultipartFile(
            "file", 
            "large_image.jpg", 
            "image/jpeg", 
            largeContent
        );

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        
        String result = fileService.uploadAvatar(file, userId);

         
        assertNotNull(result);
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void init_ShouldCreateBucketAndSetPolicy_WhenBucketDoesNotExist() throws Exception {
         
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        
        fileService.init();

         
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));
    }

    @Test
    void init_ShouldOnlySetPolicy_WhenBucketExists() throws Exception {
         
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        
        fileService.init();
         
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));
    }

    @Test
    void init_ShouldSetCorrectPublicReadPolicy() throws Exception {
         
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        ArgumentCaptor<SetBucketPolicyArgs> captor = ArgumentCaptor.forClass(SetBucketPolicyArgs.class);

        
        fileService.init();

         
        verify(minioClient).setBucketPolicy(captor.capture());
        SetBucketPolicyArgs capturedArgs = captor.getValue();
        
        assertEquals(BUCKET_NAME, capturedArgs.bucket());
        String policy = capturedArgs.config();
        assertNotNull(policy);
        assertTrue(policy.contains(BUCKET_NAME));
        assertTrue(policy.contains("s3:GetObject"));
        assertTrue(policy.contains("Allow"));
        assertTrue(policy.contains("*"));
    }

    @Test
    void init_ShouldHandleExceptionGracefully() throws Exception {
         
        String errorMessage = "Failed to check bucket existence";
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
            .thenThrow(new RuntimeException(errorMessage));

        assertDoesNotThrow(() -> fileService.init());
        
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient, never()).setBucketPolicy(any(SetBucketPolicyArgs.class));
    }

    @Test
    void uploadAvatar_ShouldUseCorrectInputStream() throws Exception {
         
        Long userId = 555L;
        byte[] content = "unique test content".getBytes();
        
        MultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            "image/jpeg", 
            content
        );

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        
        fileService.uploadAvatar(file, userId);

         
        verify(minioClient).putObject(captor.capture());
        PutObjectArgs capturedArgs = captor.getValue();
        
        assertEquals(content.length, capturedArgs.objectSize());
        assertNotNull(capturedArgs.stream());
    }
}