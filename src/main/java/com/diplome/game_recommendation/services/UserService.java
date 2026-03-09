package com.diplome.game_recommendation.services;

import com.diplome.game_recommendation.core.configuration.UserAuthenticationProvider;
import com.diplome.game_recommendation.core.configuration.UserMapper;
import com.diplome.game_recommendation.core.exceptions.NotFoundException;
import com.diplome.game_recommendation.dtos.CredentialsDto;
import com.diplome.game_recommendation.dtos.UserDto;
import com.diplome.game_recommendation.core.exceptions.AppException;
import com.diplome.game_recommendation.models.UserEntity;
import org.springframework.data.domain.Pageable;
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
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class UserService {
    public final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserAuthenticationProvider userAuthenticationProvider;
    private final SecureRandom random = new SecureRandom();
    private static final String LOG_RESPONSE = "Ответ: {}";
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Value("${app.default-password}")
    private String defaultPassword;
    private final UserService self;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder, UserMapper userMapper,
            @Lazy UserAuthenticationProvider userAuthenticationProvider,
            @Lazy UserService self) {
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.repository = repository;
        this.self = self;
        this.userAuthenticationProvider = userAuthenticationProvider;
    }

    private void checkEmail(Long id, String email) {
        logger.info("Проверка существования пользователя: id={}", id);
        final Optional<UserEntity> existsUser = repository.findByEmailIgnoreCase(email);
        if (existsUser.isPresent() && !existsUser.get().getId().equals(id)) {
            logger.warn("Пользователь с такой почтой уже существует");
            throw new IllegalArgumentException("Пользователь с такой почтой уже существует");
        }
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
    public UserEntity getByLogin(String login) {
        logger.info("Получение пользователя с помощью login :{}", login);
        var result = repository.findByLoginIgnoreCase(login)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login"));
        logger.info(LOG_RESPONSE, result);
        return result;
    }

    public UserDto login(CredentialsDto credentialsDto) {
        logger.info("Попытка входа: {}", credentialsDto);
        UserEntity user = repository.findByLogin(credentialsDto.getEmail())
                .orElseThrow(() -> new AppException("Unknown user", HttpStatus.NOT_FOUND));
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

    public void resetPassword(String email) {
        UserEntity user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь с таким email не найден"));

        String newPassword = generateRandomPassword(10);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        repository.save(user);

    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }
}
