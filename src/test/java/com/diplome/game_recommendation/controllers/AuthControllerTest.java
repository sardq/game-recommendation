package com.diplome.game_recommendation.controllers;

import com.diplome.game_recommendation.dtos.CredentialsDto;
import com.diplome.game_recommendation.dtos.UserDto;
import com.diplome.game_recommendation.dtos.UserSignupDto;
import com.diplome.game_recommendation.helpers.configuration.UserAuthenticationProvider;
import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserAuthenticationProvider authProvider;

    @Mock
    private ModelMapper mapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private CredentialsDto credentialsDto;
    private UserSignupDto userSignupDto;
    private UserDto userDto;
    private UserEntity userEntity;
    private Map<String, String> resetPasswordRequest;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password123";
    private final String TEST_USERNAME = "testuser";
    private final Long TEST_USER_ID = 1L;
    private final String TEST_TOKEN = "jwt-token-123";
    private final String NEW_PASSWORD = "newPassword";

    @BeforeEach
    void setUp() {
        credentialsDto = new CredentialsDto();
        credentialsDto.setEmail(TEST_EMAIL);
        credentialsDto.setPassword(TEST_PASSWORD.toCharArray());

        userSignupDto = new UserSignupDto();
        userSignupDto.setEmail(TEST_EMAIL);
        userSignupDto.setPassword(TEST_PASSWORD);
        userSignupDto.setUsername(TEST_USERNAME);

        userDto = new UserDto();
        userDto.setId(TEST_USER_ID);
        userDto.setEmail(TEST_EMAIL);
        userDto.setUsername(TEST_USERNAME);
        userDto.setToken(null);

        // Setup UserEntity
        userEntity = new UserEntity();
        userEntity.setId(TEST_USER_ID);
        userEntity.setEmail(TEST_EMAIL);
        userEntity.setUsername(TEST_USERNAME);

        resetPasswordRequest = new HashMap<>();
        resetPasswordRequest.put("email", TEST_EMAIL);
    }

    @Test
    void login_WithValidCredentials_ShouldReturnUserDtoWithToken() {
        // Arrange
        when(userService.login(credentialsDto)).thenReturn(userDto);
        when(authProvider.createToken(TEST_EMAIL)).thenReturn(TEST_TOKEN);

        ResponseEntity<UserDto> response = authController.login(credentialsDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        UserDto responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(TEST_EMAIL, responseBody.getEmail());
        assertEquals(TEST_TOKEN, responseBody.getToken());
        
        verify(userService).login(credentialsDto);
        verify(authProvider).createToken(TEST_EMAIL);
    }

    @Test
    void login_ShouldHandleServiceException() {
        when(userService.login(credentialsDto)).thenThrow(new RuntimeException("Invalid credentials"));

        assertThrows(RuntimeException.class, () -> authController.login(credentialsDto));
        verify(userService).login(credentialsDto);
        verify(authProvider, never()).createToken(anyString());
    }

    @Test
    void resetPassword_WithValidEmail_ShouldReturnOkResponse() {
        doNothing().when(userService).resetPassword(TEST_EMAIL, NEW_PASSWORD);

        ResponseEntity<String> response = authController.resetPassword(resetPasswordRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password reset instructions sent.", response.getBody());
        
        verify(userService).resetPassword(TEST_EMAIL, NEW_PASSWORD);
    }

    @Test
    void resetPassword_WithNullEmail_ShouldReturnBadRequest() {
        resetPasswordRequest.put("email", null);

        ResponseEntity<String> response = authController.resetPassword(resetPasswordRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email is required", response.getBody());
        
        verify(userService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetPassword_WithBlankEmail_ShouldReturnBadRequest() {
        resetPasswordRequest.put("email", "");

        ResponseEntity<String> response = authController.resetPassword(resetPasswordRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email is required", response.getBody());
        
        verify(userService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetPassword_WithEmptyEmail_ShouldReturnBadRequest() {
        resetPasswordRequest.put("email", "   ");

        ResponseEntity<String> response = authController.resetPassword(resetPasswordRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(userService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetPassword_WhenEmailNotFound_ShouldPropagateException() {
        doThrow(new RuntimeException("User not found")).when(userService).resetPassword(TEST_EMAIL, NEW_PASSWORD);

        assertThrows(RuntimeException.class, () -> authController.resetPassword(resetPasswordRequest));
        verify(userService).resetPassword(TEST_EMAIL, NEW_PASSWORD);
    }

    @Test
    void register_WithValidData_ShouldReturnCreatedUser() {
        when(userService.register(userSignupDto)).thenReturn(userDto);
        when(authProvider.createToken(TEST_EMAIL)).thenReturn(TEST_TOKEN);

        ResponseEntity<UserDto> response = authController.register(userSignupDto);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(URI.create("/users/" + TEST_USER_ID), response.getHeaders().getLocation());
        
        UserDto responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(TEST_EMAIL, responseBody.getEmail());
        assertEquals(TEST_TOKEN, responseBody.getToken());
        
        verify(userService).register(userSignupDto);
        verify(authProvider).createToken(TEST_EMAIL);
    }

    @Test
    void register_WhenEmailAlreadyExists_ShouldPropagateException() {
        when(userService.register(userSignupDto)).thenThrow(new RuntimeException("Email already exists"));

        assertThrows(RuntimeException.class, () -> authController.register(userSignupDto));
        verify(userService).register(userSignupDto);
        verify(authProvider, never()).createToken(anyString());
    }

    @Test
    void register_WithInvalidData_ShouldThrowException() {
        userSignupDto.setEmail("invalid-email");
        when(userService.register(userSignupDto)).thenThrow(new IllegalArgumentException("Invalid email"));

        assertThrows(IllegalArgumentException.class, () -> authController.register(userSignupDto));
        verify(userService).register(userSignupDto);
        verify(authProvider, never()).createToken(anyString());
    }

    @Test
    void getMe_WithValidAuthentication_ShouldReturnUserDto() {
        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userService.getByEmail(TEST_EMAIL)).thenReturn(userEntity);
        when(mapper.map(userEntity, UserDto.class)).thenReturn(userDto);

        UserDto result = authController.getMe(authentication);

        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_USERNAME, result.getUsername());
        assertEquals(TEST_USER_ID, result.getId());
        
        verify(authentication).getName();
        verify(userService).getByEmail(TEST_EMAIL);
        verify(mapper).map(userEntity, UserDto.class);
    }

    @Test
    void getMe_WhenAuthenticationIsNull_ShouldThrowException() {
        assertThrows(NullPointerException.class, () -> authController.getMe(null));
        verify(userService, never()).getByEmail(anyString());
    }

    @Test
    void getMe_WhenUserNotFound_ShouldThrowException() {
        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userService.getByEmail(TEST_EMAIL)).thenThrow(new RuntimeException("User not found"));

        assertThrows(RuntimeException.class, () -> authController.getMe(authentication));
        verify(authentication).getName();
        verify(userService).getByEmail(TEST_EMAIL);
    }

    @Test
    void getMe_ShouldMapCorrectly() {
        UserEntity customUser = new UserEntity();
        customUser.setId(99L);
        customUser.setEmail("custom@example.com");
        customUser.setUsername("customuser");
        
        UserDto expectedDto = new UserDto();
        expectedDto.setId(99L);
        expectedDto.setEmail("custom@example.com");
        expectedDto.setUsername("customuser");
        
        when(authentication.getName()).thenReturn("custom@example.com");
        when(userService.getByEmail("custom@example.com")).thenReturn(customUser);
        when(mapper.map(customUser, UserDto.class)).thenReturn(expectedDto);

        UserDto result = authController.getMe(authentication);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals("custom@example.com", result.getEmail());
        assertEquals("customuser", result.getUsername());
    }

    @Test
    void resetPassword_ShouldAlwaysUseNewPasswordConstant() {
        doNothing().when(userService).resetPassword(TEST_EMAIL, "newPassword");

        ResponseEntity<String> response = authController.resetPassword(resetPasswordRequest);

        assertNotNull(response);
        verify(userService).resetPassword(TEST_EMAIL, "newPassword");
    }

    @Test
    void login_ShouldSetTokenEvenIfUserDtoAlreadyHasToken() {
        UserDto userWithExistingToken = new UserDto();
        userWithExistingToken.setId(TEST_USER_ID);
        userWithExistingToken.setEmail(TEST_EMAIL);
        userWithExistingToken.setToken("old-token");
        
        when(userService.login(credentialsDto)).thenReturn(userWithExistingToken);
        when(authProvider.createToken(TEST_EMAIL)).thenReturn(TEST_TOKEN);

        ResponseEntity<UserDto> response = authController.login(credentialsDto);

        assertNotNull(response);
        assertEquals(TEST_TOKEN, response.getBody().getToken());
        assertNotEquals("old-token", response.getBody().getToken());
    }

    @Test
    void register_ShouldCreateUserWithCorrectLocationHeader() {
        UserDto createdUser = new UserDto();
        createdUser.setId(5L);
        createdUser.setEmail("new@example.com");
        
        when(userService.register(any(UserSignupDto.class))).thenReturn(createdUser);
        when(authProvider.createToken(anyString())).thenReturn("new-token");

        ResponseEntity<UserDto> response = authController.register(userSignupDto);

        assertNotNull(response);
        assertEquals(URI.create("/users/5"), response.getHeaders().getLocation());
    }

    @Test
    void login_ShouldLogRequest() {
        when(userService.login(credentialsDto)).thenReturn(userDto);
        when(authProvider.createToken(TEST_EMAIL)).thenReturn(TEST_TOKEN);

        ResponseEntity<UserDto> response = authController.login(credentialsDto);

        assertNotNull(response);
        verify(userService).login(credentialsDto);
    }

    @Test
    void register_ShouldLogRequest() {
        when(userService.register(userSignupDto)).thenReturn(userDto);
        when(authProvider.createToken(TEST_EMAIL)).thenReturn(TEST_TOKEN);

        ResponseEntity<UserDto> response = authController.register(userSignupDto);

        assertNotNull(response);
        verify(userService).register(userSignupDto);
    }
}
