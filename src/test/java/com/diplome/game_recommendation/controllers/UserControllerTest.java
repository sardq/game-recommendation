package com.diplome.game_recommendation.controllers;

import com.diplome.game_recommendation.dtos.PublicUserDto;
import com.diplome.game_recommendation.dtos.ReviewDto;
import com.diplome.game_recommendation.dtos.UserDto;
import com.diplome.game_recommendation.helpers.configuration.Constants;
import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    private UserEntity testUser1;
    private UserEntity testUser2;
    private UserDto testUserDto1;
    private UserDto testUserDto2;
    private PublicUserDto testPublicUserDto;
    private UserGames testUserGame1;
    private UserGames testUserGame2;

    private final Long TEST_USER_ID_1 = 1L;
    private final Long TEST_USER_ID_2 = 2L;
    private final Long TEST_USER_ID_3 = 3L;
    private final String TEST_EMAIL_1 = "user1@example.com";
    private final String TEST_EMAIL_2 = "user2@example.com";
    private final String TEST_USERNAME_1 = "user1";
    private final String TEST_USERNAME_2 = "user2";
    private final String TEST_AVATAR_URL = "http://minio.com/bucket/avatars/1_avatar.jpg";
    private final String TEST_NEW_AVATAR_URL = "http://minio.com/bucket/avatars/1_new_avatar.jpg";

    @BeforeEach
    void setUp() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

        // Setup test UserEntity objects
        testUser1 = new UserEntity();
        testUser1.setId(TEST_USER_ID_1);
        testUser1.setEmail(TEST_EMAIL_1);
        testUser1.setUsername(TEST_USERNAME_1);
        testUser1.setAvatarUrl(TEST_AVATAR_URL);
        testUser1.setBirthDate(formatter.parse("01-01-1990"));
        testUser1.setRegistrationDate(LocalDate.now());

        testUser2 = new UserEntity();
        testUser2.setId(TEST_USER_ID_2);
        testUser2.setEmail(TEST_EMAIL_2);
        testUser2.setUsername(TEST_USERNAME_2);
        testUser2.setAvatarUrl(null);
        testUser2.setBirthDate(formatter.parse("15-05-1995"));
        testUser2.setRegistrationDate(LocalDate.now());

        // Setup test UserDto objects
        testUserDto1 = new UserDto();
        testUserDto1.setId(TEST_USER_ID_1);
        testUserDto1.setEmail(TEST_EMAIL_1);
        testUserDto1.setUsername(TEST_USERNAME_1);
        testUserDto1.setAvatarUrl(TEST_AVATAR_URL);
        testUserDto1.setBirthDate(formatter.parse("01-01-1990"));

        testUserDto2 = new UserDto();
        testUserDto2.setId(TEST_USER_ID_2);
        testUserDto2.setEmail(TEST_EMAIL_2);
        testUserDto2.setUsername(TEST_USERNAME_2);
        testUserDto2.setAvatarUrl(null);
        testUserDto2.setBirthDate(formatter.parse("05-05-1995"));

        // Setup test PublicUserDto
        testPublicUserDto = new PublicUserDto();
        testPublicUserDto.setUsername(TEST_USERNAME_1);
        testPublicUserDto.setAvatarUrl(TEST_AVATAR_URL);
        testPublicUserDto.setReviews(List.of(new ReviewDto(), new ReviewDto()));

        // Setup test UserGames objects
        testUserGame1 = new UserGames();
        testUserGame1.setId(1L);
        testUserGame1.setTime(LocalDateTime.now());
        
        testUserGame2 = new UserGames();
        testUserGame2.setId(2L);
        testUserGame2.setTime(LocalDateTime.now().minusDays(1));
    }

    @Test
    void getAll_ShouldReturnListOfUserDtos() {
        // Arrange
        int page = 0;
        int size = 10;
        PageRequest pageable = PageRequest.of(page, size);
        List<UserEntity> users = Arrays.asList(testUser1, testUser2);
        Page<UserEntity> userPage = new PageImpl<>(users, pageable, users.size());

        when(userService.getAll(page, size)).thenReturn(userPage);
        when(modelMapper.map(testUser1, UserDto.class)).thenReturn(testUserDto1);
        when(modelMapper.map(testUser2, UserDto.class)).thenReturn(testUserDto2);

        // Act
        List<UserDto> result = userController.getAll(page, size);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(TEST_USER_ID_1, result.get(0).getId());
        assertEquals(TEST_EMAIL_1, result.get(0).getEmail());
        assertEquals(TEST_USERNAME_1, result.get(0).getUsername());
        assertEquals(TEST_AVATAR_URL, result.get(0).getAvatarUrl());
        
        assertEquals(TEST_USER_ID_2, result.get(1).getId());
        assertEquals(TEST_EMAIL_2, result.get(1).getEmail());
        assertEquals(TEST_USERNAME_2, result.get(1).getUsername());
        assertNull(result.get(1).getAvatarUrl());
        
        verify(userService).getAll(page, size);
        verify(modelMapper, times(2)).map(any(UserEntity.class), eq(UserDto.class));
    }

    @Test
    void getAll_ShouldUseDefaultPagination() {
        // Arrange
        int defaultPage = 0;
        int defaultSize = 10;
        PageRequest pageable = PageRequest.of(defaultPage, defaultSize);
        Page<UserEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userService.getAll(defaultPage, defaultSize)).thenReturn(emptyPage);

        // Act
        List<UserDto> result = userController.getAll(defaultPage, defaultSize);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userService).getAll(defaultPage, defaultSize);
    }

    @Test
    void getAll_ShouldHandleEmptyResult() {
        // Arrange
        int page = 0;
        int size = 10;
        PageRequest pageable = PageRequest.of(page, size);
        Page<UserEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userService.getAll(page, size)).thenReturn(emptyPage);

        // Act
        List<UserDto> result = userController.getAll(page, size);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void getAll_ShouldHandleLargePageSize() {
        // Arrange
        int page = 0;
        int size = 1000;
        PageRequest pageable = PageRequest.of(page, size);
        
        List<UserEntity> manyUsers = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            UserEntity user = new UserEntity();
            user.setId((long) i);
            manyUsers.add(user);
        }
        Page<UserEntity> userPage = new PageImpl<>(manyUsers, pageable, manyUsers.size());

        when(userService.getAll(page, size)).thenReturn(userPage);
        
        // Mock mapper for each user
        for (UserEntity user : manyUsers) {
            UserDto dto = new UserDto();
            dto.setId(user.getId());
            when(modelMapper.map(user, UserDto.class)).thenReturn(dto);
        }

        // Act
        List<UserDto> result = userController.getAll(page, size);

        // Assert
        assertNotNull(result);
        assertEquals(size, result.size());
    }

    @Test
    void getById_ShouldReturnUserDto() {
        // Arrange
        when(userService.get(TEST_USER_ID_1)).thenReturn(testUser1);
        when(modelMapper.map(testUser1, UserDto.class)).thenReturn(testUserDto1);

        // Act
        UserDto result = userController.getById(TEST_USER_ID_1);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USER_ID_1, result.getId());
        assertEquals(TEST_EMAIL_1, result.getEmail());
        assertEquals(TEST_USERNAME_1, result.getUsername());
        assertEquals(TEST_AVATAR_URL, result.getAvatarUrl());
        
        verify(userService).get(TEST_USER_ID_1);
        verify(modelMapper).map(testUser1, UserDto.class);
    }

    @Test
    void getById_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userService.get(TEST_USER_ID_1)).thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userController.getById(TEST_USER_ID_1));
        verify(userService).get(TEST_USER_ID_1);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void getById_WithInvalidId_ShouldThrowException() {
        // Arrange
        Long invalidId = -1L;
        when(userService.get(invalidId)).thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userController.getById(invalidId));
        verify(userService).get(invalidId);
    }

    @Test
    void history_ShouldReturnUserHistory() {
        // Arrange
        List<UserGames> history = Arrays.asList(testUserGame1, testUserGame2);
        when(userService.getUserHistory(TEST_USER_ID_1)).thenReturn(history);

        // Act
        List<UserGames> result = userController.history(TEST_USER_ID_1);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        
        verify(userService).getUserHistory(TEST_USER_ID_1);
    }

    @Test
    void history_WhenNoHistory_ShouldReturnEmptyList() {
        // Arrange
        when(userService.getUserHistory(TEST_USER_ID_1)).thenReturn(List.of());

        // Act
        List<UserGames> result = userController.history(TEST_USER_ID_1);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userService).getUserHistory(TEST_USER_ID_1);
    }

    @Test
    void updateProfile_ShouldReturnUpdatedUserDto() throws ParseException {
        // Arrange
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

        UserDto updateDto = new UserDto();
        updateDto.setBirthDate(formatter.parse("01-01-2000"));
        
        UserDto updatedUserDto = new UserDto();
        updatedUserDto.setId(TEST_USER_ID_1);
        updatedUserDto.setEmail(TEST_EMAIL_1);
        updatedUserDto.setBirthDate(formatter.parse("01-01-2000"));
        
        when(authentication.getName()).thenReturn(TEST_EMAIL_1);
        when(userService.updateProfile(TEST_EMAIL_1, updateDto)).thenReturn(updatedUserDto);

        // Act
        ResponseEntity<UserDto> response = userController.updateProfile(updateDto, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        UserDto result = response.getBody();
        assertNotNull(result);
        assertEquals(TEST_USER_ID_1, result.getId());
        assertEquals(formatter.parse("01-01-2000"), result.getBirthDate());
        
        verify(userService).updateProfile(TEST_EMAIL_1, updateDto);
    }

    @Test
    void updateProfile_WithNullBirthDate_ShouldNotUpdateBirthDate() throws ParseException {
        // Arrange
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

        UserDto updateDto = new UserDto();
        updateDto.setBirthDate(null);
        
        when(authentication.getName()).thenReturn(TEST_EMAIL_1);
        when(userService.updateProfile(TEST_EMAIL_1, updateDto)).thenReturn(testUserDto1);

        // Act
        ResponseEntity<UserDto> response = userController.updateProfile(updateDto, authentication);

        // Assert
        assertNotNull(response);
        UserDto result = response.getBody();
        assertNotNull(result);
        assertEquals(formatter.parse("01-01-1990"), result.getBirthDate());
    }

    @Test
    void updateProfile_WhenUserNotFound_ShouldThrowException() throws ParseException {
        // Arrange
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

        UserDto updateDto = new UserDto();
        updateDto.setBirthDate(formatter.parse("01-01-2000"));
        
        when(authentication.getName()).thenReturn(TEST_EMAIL_1);
        when(userService.updateProfile(TEST_EMAIL_1, updateDto))
            .thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> userController.updateProfile(updateDto, authentication));
        
        verify(userService).updateProfile(TEST_EMAIL_1, updateDto);
    }

    @Test
    void uploadAvatar_ShouldReturnAvatarUrl() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "avatar", 
            "avatar.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "test image content".getBytes()
        );
        
        when(authentication.getName()).thenReturn(TEST_EMAIL_1);
        when(userService.updateAvatar(TEST_EMAIL_1, file)).thenReturn(TEST_NEW_AVATAR_URL);

        // Act
        ResponseEntity<String> response = userController.uploadAvatar(file, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(TEST_NEW_AVATAR_URL, response.getBody());
        
        verify(userService).updateAvatar(TEST_EMAIL_1, file);
    }

    @Test
    void uploadAvatar_WithEmptyFile_ShouldStillProcess() {
        // Arrange
        MultipartFile emptyFile = new MockMultipartFile(
            "avatar", 
            "empty.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            new byte[0]
        );
        
        when(authentication.getName()).thenReturn(TEST_EMAIL_1);
        when(userService.updateAvatar(TEST_EMAIL_1, emptyFile)).thenReturn(TEST_NEW_AVATAR_URL);

        // Act
        ResponseEntity<String> response = userController.uploadAvatar(emptyFile, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService).updateAvatar(TEST_EMAIL_1, emptyFile);
    }

    @Test
    void uploadAvatar_WhenServiceFails_ShouldThrowException() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "avatar", 
            "avatar.jpg", 
            MediaType.IMAGE_JPEG_VALUE, 
            "test content".getBytes()
        );
        
        when(authentication.getName()).thenReturn(TEST_EMAIL_1);
        when(userService.updateAvatar(TEST_EMAIL_1, file))
            .thenThrow(new RuntimeException("Upload failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> userController.uploadAvatar(file, authentication));
        
        verify(userService).updateAvatar(TEST_EMAIL_1, file);
    }

    @Test
    void getPublicProfile_ShouldReturnPublicUserDto() {
        // Arrange
        when(userService.getPublicProfile(TEST_USER_ID_3)).thenReturn(testPublicUserDto);

        // Act
        ResponseEntity<PublicUserDto> response = userController.getPublicProfile(TEST_USER_ID_3);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        PublicUserDto result = response.getBody();
        assertNotNull(result);
        assertEquals(TEST_USERNAME_1, result.getUsername());
        assertEquals(TEST_AVATAR_URL, result.getAvatarUrl());
        assertEquals(2, result.getReviews().size());
        
        verify(userService).getPublicProfile(TEST_USER_ID_3);
    }

    @Test
    void getPublicProfile_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userService.getPublicProfile(TEST_USER_ID_3))
            .thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> userController.getPublicProfile(TEST_USER_ID_3));
        
        verify(userService).getPublicProfile(TEST_USER_ID_3);
    }

    @Test
    void getPublicProfile_ShouldHandleUserWithNoReviews() {
        // Arrange
        PublicUserDto userWithNoReviews = new PublicUserDto();
        userWithNoReviews.setUsername(TEST_USERNAME_2);
        userWithNoReviews.setAvatarUrl(null);
        userWithNoReviews.setReviews(List.of());
        
        when(userService.getPublicProfile(TEST_USER_ID_2)).thenReturn(userWithNoReviews);

        // Act
        ResponseEntity<PublicUserDto> response = userController.getPublicProfile(TEST_USER_ID_2);

        // Assert
        assertNotNull(response);
        PublicUserDto result = response.getBody();
        assertNotNull(result);
        assertNull(result.getAvatarUrl());
        assertTrue(result.getReviews().isEmpty());
    }

    @Test
    void toDto_ShouldMapCorrectly() {
        // Test private toDto method indirectly through getById
        // Arrange
        when(userService.get(TEST_USER_ID_1)).thenReturn(testUser1);
        when(modelMapper.map(testUser1, UserDto.class)).thenReturn(testUserDto1);

        // Act
        UserDto result = userController.getById(TEST_USER_ID_1);

        // Assert
        assertNotNull(result);
        verify(modelMapper).map(testUser1, UserDto.class);
    }

    @Test
    void urlConstant_ShouldBeCorrect() {
        assertEquals("/api/users", Constants.API_URL + "/users");
    }

    @Test
    void updateProfile_ShouldHandleAuthenticationWithNullEmail() {
        // Arrange
        UserDto updateDto = new UserDto();
        when(authentication.getName()).thenReturn(null);
        when(userService.updateProfile(null, updateDto))
            .thenThrow(new IllegalArgumentException("Email cannot be null"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> userController.updateProfile(updateDto, authentication));
    }
}