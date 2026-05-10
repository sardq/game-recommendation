package com.diplome.game_recommendation.repositories;

import com.diplome.game_recommendation.integration.GigachatService;
import com.diplome.game_recommendation.models.*;
import com.diplome.game_recommendation.services.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "SPRING_MAIL_USERNAME=test@gmail.com",
    "SPRING_MAIL_PASSWORD=test",
    "GIGACHAT_KEY=test_key",
    "RAWG_API_KEY=test_key",
    "VIDEO_API_KEY=test_key",
    "NEWS_API_KEY=test_key",
    "spring.quartz.job-store-type=memory",
    "DB_USERNAME=postgres",
    "DB_PASSWORD=postgres",
    "spring.main.allow-bean-definition-overriding=true"
})
@ActiveProfiles("dev")
@Transactional
class RecommendationSessionRepositoryTest {

    @Autowired
    private RecommendationSessionRepository recommendationSessionRepository;

    @Autowired
    private UserRepository userRepository; // Предполагаем наличие репозитория пользователей

    @MockitoBean
    private GigachatService gigachatService;

    @MockitoBean
    private FileService fileService;

    private UserEntity testUser;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        // 1. Создаем уникального пользователя
        testUser = new UserEntity();
        testUser.setUsername("user_" + UUID.randomUUID().toString().substring(0, 5));
        testUser.setEmail(UUID.randomUUID() + "@test.com");
        testUser = userRepository.save(testUser);

        now = LocalDateTime.now();

        // 2. Создаем две сессии с разным временем
        // Сессия 1: создана 2 часа назад
        RecommendationSession oldSession = new RecommendationSession(testUser, now.minusHours(2));
        recommendationSessionRepository.save(oldSession);

        // Сессия 2: создана 5 минут назад (самая новая)
        RecommendationSession newSession = new RecommendationSession(testUser, now.minusMinutes(5));
        recommendationSessionRepository.save(newSession);
    }

    @Test
    void save_ShouldPersistSessionWithUserReference() {
        // Arrange
        RecommendationSession session = new RecommendationSession(testUser, LocalDateTime.now());

        // Act
        RecommendationSession saved = recommendationSessionRepository.save(session);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(testUser.getId());
    }
}