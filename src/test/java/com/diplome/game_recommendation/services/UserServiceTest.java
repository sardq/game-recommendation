package com.diplome.game_recommendation.services;

import com.diplome.game_recommendation.dtos.CredentialsDto;
import com.diplome.game_recommendation.dtos.PublicUserDto;
import com.diplome.game_recommendation.dtos.ReviewDto;
import com.diplome.game_recommendation.dtos.UserDto;
import com.diplome.game_recommendation.dtos.UserSignupDto;
import com.diplome.game_recommendation.helpers.configuration.UserAuthenticationProvider;
import com.diplome.game_recommendation.helpers.configuration.UserMapper;
import com.diplome.game_recommendation.helpers.exceptions.AppException;
import com.diplome.game_recommendation.helpers.exceptions.NotFoundException;
import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.repositories.UserGameRepository;
import com.diplome.game_recommendation.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.CharBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private InteractionService interactionService;

    @Mock
    private UserGameRepository userGameRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserAuthenticationProvider userAuthenticationProvider;

    @Mock
    private FileService fileService;

    @InjectMocks
    private UserService userService;

    private UserEntity testUser;
    private UserDto testUserDto;
    private UserSignupDto testSignupDto;
    
    private final Long TEST_USER_ID = 1L;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_USERNAME = "testuser";
    private final String TEST_PASSWORD = "password123";
    private final String TEST_TOKEN = "jwt-token-123";
    private final String TEST_AVATAR_URL = "http://minio.com/bucket/avatars/1_avatar.jpg";

    @BeforeEach
    void setUp() throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        testUser = new UserEntity();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail(TEST_EMAIL);
        testUser.setUsername(TEST_USERNAME);
        testUser.setPasswordHash("encoded_password");
        testUser.setRegistrationDate(LocalDate.now());
        testUser.setBirthDate(formatter.parse("01-01-1990"));
        testUser.setAvatarUrl(TEST_AVATAR_URL);

        testUserDto = new UserDto();
        testUserDto.setId(TEST_USER_ID);
        testUserDto.setEmail(TEST_EMAIL);
        testUserDto.setUsername(TEST_USERNAME);
        testUserDto.setToken(TEST_TOKEN);
        testUserDto.setBirthDate(formatter.parse("01-01-1990"));
        testUserDto.setAvatarUrl(TEST_AVATAR_URL);

        testSignupDto = new UserSignupDto();
        testSignupDto.setEmail(TEST_EMAIL);
        testSignupDto.setUsername(TEST_USERNAME);
        testSignupDto.setPassword(TEST_PASSWORD);

        ReflectionTestUtils.setField(userService, "minioExternalUrl", "http://minio.com");
        ReflectionTestUtils.setField(userService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(userService, "defaultPassword", "123456");
    }

    @Test
    void getAll_ShouldReturnPageOfUsers() {
        // Arrange
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<UserEntity> users = Arrays.asList(testUser, new UserEntity());
        Page<UserEntity> expectedPage = new PageImpl<>(users, pageable, users.size());

        when(repository.findAll(pageable)).thenReturn(expectedPage);

        // Act
        Page<UserEntity> result = userService.getAll(page, size);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(repository).findAll(pageable);
    }

    @Test
    void get_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(repository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

        // Act
        UserEntity result = userService.get(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getId());
        assertEquals(TEST_EMAIL, result.getEmail());
        verify(repository).findById(TEST_USER_ID);
    }

    @Test
    void get_WhenUserNotFound_ShouldThrowNotFoundException() {
        // Arrange
        when(repository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> userService.get(TEST_USER_ID));
        verify(repository).findById(TEST_USER_ID);
    }

    @Test
    void getByEmail_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(repository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // Act
        UserEntity result = userService.getByEmail(TEST_EMAIL);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
        verify(repository).findByEmailIgnoreCase(TEST_EMAIL);
    }

    @Test
    void getByEmail_WhenUserNotFound_ShouldThrowIllegalArgumentException() {
        // Arrange
        when(repository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.getByEmail(TEST_EMAIL));
        verify(repository).findByEmailIgnoreCase(TEST_EMAIL);
    }

    @Test
    void getByLogin_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(repository.findByUsernameIgnoreCase(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        // Act
        UserEntity result = userService.getByLogin(TEST_USERNAME);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USERNAME, result.getUsername());
        verify(repository).findByUsernameIgnoreCase(TEST_USERNAME);
    }

    @Test
    void getByLogin_WhenUserNotFound_ShouldThrowIllegalArgumentException() {
        // Arrange
        when(repository.findByUsernameIgnoreCase(TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.getByLogin(TEST_USERNAME));
        verify(repository).findByUsernameIgnoreCase(TEST_USERNAME);
    }

    @Test
    void login_WithValidCredentials_ShouldReturnUserDtoWithToken() {
        // Arrange
        CredentialsDto credentials = new CredentialsDto();
        credentials.setEmail(TEST_EMAIL);
        credentials.setPassword(TEST_PASSWORD.toCharArray());

        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CharBuffer.wrap(TEST_PASSWORD), testUser.getPasswordHash()))
            .thenReturn(true);
        when(userAuthenticationProvider.createToken(TEST_EMAIL)).thenReturn(TEST_TOKEN);
        when(userMapper.toUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.login(credentials);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_TOKEN, result.getToken());
        verify(repository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(any(), any());
        verify(userAuthenticationProvider).createToken(TEST_EMAIL);
        verify(userMapper).toUserDto(testUser);
    }

    @Test
    void login_WithInvalidEmail_ShouldThrowAppException() {
        // Arrange
        CredentialsDto credentials = new CredentialsDto();
        credentials.setEmail("wrong@example.com");
        credentials.setPassword(TEST_PASSWORD.toCharArray());

        when(repository.findByEmail(credentials.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, 
            () -> userService.login(credentials));
        
        assertEquals("Неизвестный пользователь", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(repository).findByEmail(credentials.getEmail());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowAppException() {
        // Arrange
        CredentialsDto credentials = new CredentialsDto();
        credentials.setEmail(TEST_EMAIL);
        credentials.setPassword("wrongpassword".toCharArray());

        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CharBuffer.wrap("wrongpassword"), testUser.getPasswordHash()))
            .thenReturn(false);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, 
            () -> userService.login(credentials));
        
        assertEquals("Неверный логин или пароль", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(userAuthenticationProvider, never()).createToken(any());
    }

    @Test
    void register_WithNewEmail_ShouldRegisterUser() {
        // Arrange
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        when(userMapper.signUpToUser(testSignupDto)).thenReturn(testUser);
        when(passwordEncoder.encode(CharBuffer.wrap(TEST_PASSWORD))).thenReturn("encoded_password");
        when(repository.save(any(UserEntity.class))).thenReturn(testUser);
        when(userMapper.toUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.register(testSignupDto);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
        
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repository).save(captor.capture());
        
        UserEntity savedUser = captor.getValue();
        assertEquals(TEST_EMAIL, savedUser.getEmail());
        assertEquals("encoded_password", savedUser.getPasswordHash());
        assertNotNull(savedUser.getRegistrationDate());
        
        verify(userMapper).signUpToUser(testSignupDto);
        verify(passwordEncoder).encode(CharBuffer.wrap(TEST_PASSWORD));
        verify(userMapper).toUserDto(testUser);
    }

    @Test
    void register_WithExistingEmail_ShouldThrowAppException() {
        // Arrange
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, 
            () -> userService.register(testSignupDto));
        
        assertEquals("Login already exists", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void getUserHistory_ShouldReturnSortedUserHistory() {
        // Arrange
        UserGames game1 = new UserGames();
        game1.setTime(LocalDateTime.now().minusDays(1));
        
        UserGames game2 = new UserGames();
        game2.setTime(LocalDateTime.now());
        
        UserGames game3 = new UserGames();
        game3.setTime(LocalDateTime.now().minusDays(2));
        
        List<UserGames> history = Arrays.asList(game1, game2, game3);
        
        when(userGameRepository.findByUserId(TEST_USER_ID)).thenReturn(history);

        // Act
        List<UserGames> result = userService.getUserHistory(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        // Should be sorted descending by time
        assertTrue(result.get(0).getTime().isAfter(result.get(1).getTime()));
        assertTrue(result.get(1).getTime().isAfter(result.get(2).getTime()));
        verify(userGameRepository).findByUserId(TEST_USER_ID);
    }

    @Test
    void getUserHistory_WhenNoHistory_ShouldReturnEmptyList() {
        // Arrange
        when(userGameRepository.findByUserId(TEST_USER_ID)).thenReturn(new ArrayList<>());

        // Act
        List<UserGames> result = userService.getUserHistory(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userGameRepository).findByUserId(TEST_USER_ID);
    }

    @Test
    void resetPassword_ShouldResetPassword() {
        // Arrange
        String newPassword = "newPassword123";
        
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(repository.save(any(UserEntity.class))).thenReturn(testUser);

        // Act
        userService.resetPassword(TEST_EMAIL, newPassword);

        // Assert
        verify(repository).findByEmail(TEST_EMAIL);
        verify(repository).save(testUser);
    }

    @Test
    void resetPassword_WhenEmailNotFound_ShouldThrowRuntimeException() {
        // Arrange
        String newPassword = "newPassword123";
        
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> userService.resetPassword(TEST_EMAIL, newPassword));
        
        verify(repository, never()).save(any());
    }

    @Test
    void updateProfile_ShouldUpdateUserBirthDate() throws ParseException {
        // Arrange
        UserDto updateDto = new UserDto();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        updateDto.setBirthDate(formatter.parse("15-05-1995"));
        
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(repository.save(any(UserEntity.class))).thenReturn(testUser);
        when(userMapper.toUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.updateProfile(TEST_EMAIL, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(formatter.parse("15-05-1995"), testUser.getBirthDate());
        verify(repository).save(testUser);
        verify(userMapper).toUserDto(testUser);
    }

    @Test
    void updateProfile_WithNullBirthDate_ShouldNotUpdate() throws ParseException {
        // Arrange
        UserDto updateDto = new UserDto();
        updateDto.setBirthDate(null);
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(repository.save(any(UserEntity.class))).thenReturn(testUser);
        when(userMapper.toUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.updateProfile(TEST_EMAIL, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(formatter.parse("01-01-1990"), testUser.getBirthDate()); // Original date unchanged
        verify(repository).save(testUser);
    }

    @Test
    void updateProfile_WhenUserNotFound_ShouldThrowRuntimeException() throws ParseException {
        // Arrange
        UserDto updateDto = new UserDto();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

        updateDto.setBirthDate(formatter.parse("15-05-1995"));
        
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> userService.updateProfile(TEST_EMAIL, updateDto));
        
        verify(repository, never()).save(any());
    }

    @Test
    void updateAvatar_ShouldUploadAndUpdateAvatarUrl() throws Exception {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "avatar", 
            "avatar.jpg", 
            "image/jpeg", 
            "test image content".getBytes()
        );
        
        String uploadedFileName = "avatars/1_123456789_avatar.jpg";
        
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(fileService.uploadAvatar(file, TEST_USER_ID)).thenReturn(uploadedFileName);
        when(repository.save(any(UserEntity.class))).thenReturn(testUser);

        // Act
        String result = userService.updateAvatar(TEST_EMAIL, file);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(uploadedFileName));
        assertTrue(result.contains("http://minio.com"));
        assertTrue(result.contains("test-bucket"));
        
        verify(fileService).uploadAvatar(file, TEST_USER_ID);
        verify(repository).save(testUser);
    }

    @Test
    void updateAvatar_WhenUserNotFound_ShouldThrowException() throws Exception {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "avatar", 
            "avatar.jpg", 
            "image/jpeg", 
            "test image content".getBytes()
        );
        
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, 
            () -> userService.updateAvatar(TEST_EMAIL, file));
        
        verify(fileService, never()).uploadAvatar(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void getPublicProfile_ShouldReturnUserPublicProfile() {
        // Arrange
        List<ReviewDto> reviews = Arrays.asList(new ReviewDto(), new ReviewDto());
        
        when(repository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(interactionService.getReviewsByUserId(TEST_USER_ID)).thenReturn(reviews);

        // Act
        PublicUserDto result = userService.getPublicProfile(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USERNAME, result.getUsername());
        assertEquals(TEST_AVATAR_URL, result.getAvatarUrl());
        assertEquals(2, result.getReviews().size());
        
        verify(repository).findById(TEST_USER_ID);
        verify(interactionService).getReviewsByUserId(TEST_USER_ID);
    }

    @Test
    void getPublicProfile_WhenUserNotFound_ShouldThrowRuntimeException() {
        // Arrange
        when(repository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> userService.getPublicProfile(TEST_USER_ID));
        
        verify(interactionService, never()).getReviewsByUserId(any());
    }

    @Test
    void getPublicProfile_WhenUserHasNoReviews_ShouldReturnEmptyList() {
        // Arrange
        when(repository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(interactionService.getReviewsByUserId(TEST_USER_ID)).thenReturn(new ArrayList<>());

        // Act
        PublicUserDto result = userService.getPublicProfile(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USERNAME, result.getUsername());
        assertTrue(result.getReviews().isEmpty());
    }

    @Test
    void getAll_ShouldHandleLargePageSize() {
        // Arrange
        int page = 0;
        int size = 1000;
        Pageable pageable = PageRequest.of(page, size);
        
        List<UserEntity> users = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            UserEntity user = new UserEntity();
            user.setId((long) i);
            users.add(user);
        }
        Page<UserEntity> expectedPage = new PageImpl<>(users, pageable, users.size());

        when(repository.findAll(pageable)).thenReturn(expectedPage);

        // Act
        Page<UserEntity> result = userService.getAll(page, size);

        // Assert
        assertNotNull(result);
        assertEquals(size, result.getContent().size());
        verify(repository).findAll(pageable);
    }

    @Test
    void getUserHistory_ShouldHandleHistoryWithSameTimestamps() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        UserGames game1 = new UserGames();
        game1.setTime(now);
        
        UserGames game2 = new UserGames();
        game2.setTime(now);
        
        List<UserGames> history = Arrays.asList(game1, game2);
        
        when(userGameRepository.findByUserId(TEST_USER_ID)).thenReturn(history);

        // Act
        List<UserGames> result = userService.getUserHistory(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        // Should not throw exception with equal timestamps
    }

    @Test
    void login_ShouldHandleEmptyPassword() {
        // Arrange
        CredentialsDto credentials = new CredentialsDto();
        credentials.setEmail(TEST_EMAIL);
        credentials.setPassword("".toCharArray());
        
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CharBuffer.wrap(""), testUser.getPasswordHash()))
            .thenReturn(false);

        // Act & Assert
        assertThrows(AppException.class, () -> userService.login(credentials));
    }

    @Test
    void register_ShouldSetRegistrationDate() {
        // Arrange
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        when(userMapper.signUpToUser(testSignupDto)).thenReturn(testUser);
        when(passwordEncoder.encode(any())).thenReturn("encoded_password");
        when(repository.save(any(UserEntity.class))).thenReturn(testUser);
        when(userMapper.toUserDto(testUser)).thenReturn(testUserDto);

        // Act
        userService.register(testSignupDto);

        // Assert
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repository).save(captor.capture());
        
        UserEntity savedUser = captor.getValue();
        assertNotNull(savedUser.getRegistrationDate());
        assertEquals(LocalDate.now(), savedUser.getRegistrationDate());
    }
}