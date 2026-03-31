package com.diplome.game_recommendation.controllers;

import java.net.URI;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.diplome.game_recommendation.dtos.CredentialsDto;
import com.diplome.game_recommendation.dtos.TagDto;
import com.diplome.game_recommendation.dtos.UserDto;
import com.diplome.game_recommendation.dtos.UserSignupDto;
import com.diplome.game_recommendation.helpers.configuration.UserAuthenticationProvider;
import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.services.UserService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final UserAuthenticationProvider authProvider;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final ModelMapper mapper;

    public AuthController(UserService userService,
                          UserAuthenticationProvider authProvider, ModelMapper mapper) {
        this.userService = userService;
        this.authProvider = authProvider;
        this.mapper = mapper;
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody @Valid CredentialsDto credentialsDto) {
        logger.info("Запрос на вход пользователя: {}", credentialsDto.getEmail());
        UserDto userDto = userService.login(credentialsDto);
        String token = authProvider.createToken(
                userDto.getEmail()
        );
        userDto.setToken(token);
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        logger.info("Запрос на восстановление пароля");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        userService.resetPassword(email, "newPassword"); 
        return ResponseEntity.ok("Password reset instructions sent.");
    }
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody @Valid UserSignupDto user) {
        logger.info("Запрос на регистрацию");
        UserDto createdUser = userService.register(user);
        createdUser.setToken(authProvider.createToken(user.getEmail()));
        return ResponseEntity.created(URI.create("/users/" + createdUser.getId())).body(createdUser);
    }
    @GetMapping("me")
    public UserDto getMe(Authentication authentication) {
       return toDto(userService.getByEmail(authentication.getName()));
    }
    private UserDto toDto(UserEntity entity) {
        return mapper.map(entity, UserDto.class);
    }
    
}