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

import java.util.Date;
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
class RecommendationItemsRepositoryTest {

    @Autowired
    private RecommendationItemsRepository recommendationItemsRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private RecommendationSessionRepository sessionRepository; // Предполагаем наличие этого репозитория

    @MockitoBean
    private GigachatService gigachatService;

    @MockitoBean
    private FileService fileService;

    private RecommendationSession testSession;
    private GameEntity game1;
    private GameEntity game2;

    @BeforeEach
    void setUp() {
        // 1. Создаем и сохраняем сессию
        testSession = new RecommendationSession();
        // Установите необходимые поля для сессии, если они есть
        testSession = sessionRepository.save(testSession);

        // 2. Создаем две разные игры
        game1 = createTestGame("Witcher 3", 101L);
        game2 = createTestGame("Cyberpunk", 102L);

        // 3. Создаем айтемы рекомендаций (в неправильном порядке по рангу)
        RecommendationItems item2 = new RecommendationItems(game2, testSession, 2, 0.85);
        RecommendationItems item1 = new RecommendationItems(game1, testSession, 1, 0.95);

        recommendationItemsRepository.save(item2);
        recommendationItemsRepository.save(item1);
    }

    private GameEntity createTestGame(String name, Long rawgId) {
        GameEntity game = new GameEntity();
        game.setName(name + "_" + UUID.randomUUID().toString().substring(0, 5));
        game.setRawgId(rawgId + (long)(Math.random() * 1000));
        game.setReleaseDate(new Date());
        return gameRepository.save(game);
    }


    @Test
    void save_ShouldPersistItemWithScore() {
        // Arrange
        GameEntity game3 = createTestGame("Doom", 103L);
        RecommendationItems newItem = new RecommendationItems(game3, testSession, 3, 0.75);

        // Act
        RecommendationItems saved = recommendationItemsRepository.save(newItem);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getScore()).isEqualTo(0.75);
        assertThat(saved.getSession().getId()).isEqualTo(testSession.getId());
    }
}