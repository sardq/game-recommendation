package com.diplome.game_recommendation.services;

import com.diplome.game_recommendation.core.configuration.UserAuthenticationProvider;
import com.diplome.game_recommendation.core.configuration.UserMapper;
import com.diplome.game_recommendation.core.exceptions.NotFoundException;
import com.diplome.game_recommendation.dtos.CredentialsDto;
import com.diplome.game_recommendation.dtos.UserDto;
import com.diplome.game_recommendation.dtos.UserSignupDto;
import com.diplome.game_recommendation.core.exceptions.AppException;
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

import java.nio.CharBuffer;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    public final UserRepository repository;
    public final UserGameRepository userGameRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserAuthenticationProvider userAuthenticationProvider;
    private static final String LOG_RESPONSE = "Ответ: {}";
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Value("${DEFAULT_PASSWORD}")
    private String defaultPassword;

    public UserService(UserRepository repository,UserGameRepository userGameRepository, PasswordEncoder passwordEncoder, UserMapper userMapper,
            @Lazy UserAuthenticationProvider userAuthenticationProvider) {
        this.passwordEncoder = passwordEncoder;
        this.userGameRepository = userGameRepository;
        this.userMapper = userMapper;
        this.repository = repository;
        this.userAuthenticationProvider = userAuthenticationProvider;
    }
    @Transactional(readOnly = true)
    public Page<UserEntity> getAll(int page, int size) {
        logger.info("Получение тегов: {}, {}", page, size);
        var result = repository.findAll(PageRequest.of(page, size));
        logger.info(LOG_RESPONSE, result);
        return result;
    }
    @Transactional(readOnly = true)
    public UserEntity get(Long id) {
        logger.info("Получение пользователя: {}", id);
        var result = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(UserEntity.class, id));
        logger.info(LOG_RESPONSE, result);
        return result;

    }

    @Transactional(readOnly = true)
    public UserEntity getByEmail(String email) {
        logger.info("Получение пользователя с помощью почты :{}", email);
        var result = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email"));
        logger.info(LOG_RESPONSE, result);
        return result;
    }

    @Transactional(readOnly = true)
    public UserEntity getByLogin(String username) {
        logger.info("Получение пользователя с помощью username :{}", username);
        var result = repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username"));
        logger.info(LOG_RESPONSE, result);
        return result;
    }

    public UserDto login(CredentialsDto credentialsDto) {
        logger.info("Попытка входа: {}", credentialsDto);
        UserEntity user = repository.findByEmail(credentialsDto.getEmail())
                .orElseThrow(() -> new AppException("Неизвестный пользователь", HttpStatus.NOT_FOUND));
        boolean passwordMatches = passwordEncoder.matches(
                CharBuffer.wrap(credentialsDto.getPassword()),
                user.getPasswordHash());
        if (!passwordMatches) {
            logger.warn("Неверный пароль для пользователя");
            throw new AppException("Неверный логин или пароль", HttpStatus.BAD_REQUEST);
        }
        logger.info("Успешный вход пользователя: {} ",
                user.getEmail());
        String token = userAuthenticationProvider.createToken(
                user.getEmail());
        UserDto userDto = userMapper.toUserDto(user);
        userDto.setToken(token);
        logger.debug("Создан токен для пользователя: {}",
                user.getEmail() );
        return userDto;
    }
    public UserDto register(UserSignupDto UserEntity) {
        logger.info("Попытка регистрации: {}", UserEntity);

        Optional<UserEntity> optionalUser = repository.findByEmail(UserEntity.getEmail());

        if (optionalUser.isPresent()) {
            throw new AppException("Login already exists", HttpStatus.BAD_REQUEST);
        }
        UserEntity user = userMapper.signUpToUser(UserEntity);
        user.setPasswordHash(passwordEncoder.encode(CharBuffer.wrap(UserEntity.getPassword())));
        user.setRegistrationDate(LocalDate.now());
        UserEntity savedUser = repository.save(user);
        logger.info("Пользователь зарегистрирован: {}", savedUser);

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
}
