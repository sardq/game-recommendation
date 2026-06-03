package com.diplome.game_recommendation.services;

import com.diplome.game_recommendation.dtos.CredentialsDto;
import com.diplome.game_recommendation.dtos.PublicUserDto;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.CharBuffer;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    public final UserRepository repository;
    public final InteractionService interactionService;
    public final UserGameRepository userGameRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserAuthenticationProvider userAuthenticationProvider;
    private static final String LOG_RESPONSE = "Ответ: {}";
    private final FileService fileService;
    @Value("${DEFAULT_PASSWORD:123456}")
    private String defaultPassword;
    @Value("${minio.url}")
    private String minioExternalUrl;
    @Value("${minio.bucket}")
    private String bucket;

    public UserService(UserRepository repository,UserGameRepository userGameRepository, PasswordEncoder passwordEncoder, UserMapper userMapper,
            @Lazy UserAuthenticationProvider userAuthenticationProvider, InteractionService interactionService, FileService fileService) {
        this.passwordEncoder = passwordEncoder;
        this.userGameRepository = userGameRepository;
        this.userMapper = userMapper;
        this.interactionService = interactionService;
        this.repository = repository;
        this.userAuthenticationProvider = userAuthenticationProvider;
        this.fileService = fileService;
    }
    @Transactional(readOnly = true)
    public Page<UserEntity> getAll(int page, int size) {
        var result = repository.findAll(PageRequest.of(page, size));
        return result;
    }
    @Transactional(readOnly = true)
    public UserEntity get(Long id) {
        var result = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(UserEntity.class, id));
        return result;

    }

    @Transactional(readOnly = true)
    public UserEntity getByEmail(String email) {
        var result = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email"));
        return result;
    }

    @Transactional(readOnly = true)
    public UserEntity getByLogin(String username) {
        var result = repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username"));
        return result;
    }

    public UserDto login(CredentialsDto credentialsDto) {
        UserEntity user = repository.findByEmail(credentialsDto.getEmail())
                .orElseThrow(() -> new AppException("Неизвестный пользователь", HttpStatus.NOT_FOUND));
        boolean passwordMatches = passwordEncoder.matches(
                CharBuffer.wrap(credentialsDto.getPassword()),
                user.getPasswordHash());
        if (!passwordMatches) {
            throw new AppException("Неверный логин или пароль", HttpStatus.BAD_REQUEST);
        }
        String token = userAuthenticationProvider.createToken(
                user.getEmail());
        UserDto userDto = userMapper.toUserDto(user);
        userDto.setToken(token);
        return userDto;
    }
    public UserDto register(UserSignupDto UserEntity) {

        Optional<UserEntity> optionalUser = repository.findByEmail(UserEntity.getEmail());

        if (optionalUser.isPresent()) {
            throw new AppException("Login already exists", HttpStatus.BAD_REQUEST);
        }
        UserEntity user = userMapper.signUpToUser(UserEntity);
        user.setPasswordHash(passwordEncoder.encode(CharBuffer.wrap(UserEntity.getPassword())));
        user.setRegistrationDate(LocalDate.now());
        UserEntity savedUser = repository.save(user);

        return userMapper.toUserDto(savedUser);
    }
    public List<UserGames> getUserHistory(Long userId) {
        return userGameRepository.findByUserId(userId)
                .stream()
                .sorted((a,b) -> b.getTime().compareTo(a.getTime()))
                .toList();
    }
    public void resetPassword(String email, String newPassword) {
        UserEntity user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь с таким email не найден"));
        repository.save(user);
    }
    @Transactional
    public UserDto updateProfile(String email, UserDto userDto) {
        UserEntity user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (userDto.getBirthDate() != null) {
            user.setBirthDate(userDto.getBirthDate());
        }
        UserEntity updatedUser = repository.save(user);
        return userMapper.toUserDto(updatedUser);
    }
    @Transactional
    public String updateAvatar(String email, MultipartFile file) {
        UserEntity user = repository.findByEmail(email).orElseThrow();
        String fileName = fileService.uploadAvatar(file, user.getId());
        String fullUrl = "/storage/" + "/" + bucket + "/" + fileName;
        user.setAvatarUrl(fullUrl);
        repository.save(user);
        return fullUrl;
    }
    @Transactional(readOnly = true)
    public PublicUserDto getPublicProfile(Long userId) {
        UserEntity user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        PublicUserDto dto = new PublicUserDto();
        dto.setUsername(user.getUsername());
        dto.setAvatarUrl(user.getAvatarUrl());

        dto.setReviews(interactionService.getReviewsByUserId(userId));

        return dto;
    }
}
