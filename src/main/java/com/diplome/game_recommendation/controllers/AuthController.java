package com.diplome.game_recommendation.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.diplome.game_recommendation.core.configuration.UserAuthenticationProvider;
import com.diplome.game_recommendation.dtos.CredentialsDto;
import com.diplome.game_recommendation.dtos.UserDto;
import com.diplome.game_recommendation.services.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final UserAuthenticationProvider authProvider;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService,
                          UserAuthenticationProvider authProvider) {
        this.userService = userService;
        this.authProvider = authProvider;
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
}