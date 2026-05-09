package com.diplome.game_recommendation.controllers;


import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.diplome.game_recommendation.dtos.UserDto;
import com.diplome.game_recommendation.helpers.configuration.*;
import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.services.*;

import java.util.List;

@RestController
@RequestMapping(Constants.API_URL + "/users")
public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService, ModelMapper modelMapper) {
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

    private UserDto toDto(UserEntity entity) {
        return modelMapper.map(entity, UserDto.class);
    }

    @GetMapping
    public List<UserDto> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        logger.info("Получение пользователей page={}, size={}", page, size);

        return userService.getAll(page, size)
                .getContent()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public UserDto getById(@PathVariable Long id) {
        logger.info("Получение пользователя id={}", id);
        return toDto(userService.get(id));
    }

    @GetMapping("/{userId}/history")
    public List<UserGames> history(@PathVariable Long userId) {
        logger.info("История пользователя userId={}", userId);
        return userService.getUserHistory(userId);
    }
    @PutMapping("/update-birthdate")
    public ResponseEntity<UserDto> updateProfile(
            @RequestBody UserDto userDto, 
            Authentication authentication) {
        
        String email = authentication.getName();
        UserDto updated = userService.updateProfile(email, userDto);
        
        return ResponseEntity.ok(updated);
    }
     @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        String url = userService.updateAvatar(authentication.getName(), file);
        return ResponseEntity.ok(url);
    }
}