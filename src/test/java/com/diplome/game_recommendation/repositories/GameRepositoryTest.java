package com.diplome.game_recommendation.repositories;

import com.diplome.game_recommendation.integration.GigachatService;
import com.diplome.game_recommendation.models.*;
import com.diplome.game_recommendation.services.FileService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@Transactional // Это самое важное: все данные, созданные в setUp и @Test, откатятся (Rollback) автоматически
class GameRepositoryTest {

    @Autowired
    private GameRepository gameRepository;
    @MockitoBean
    private GigachatService gigachatService;
    @MockitoBean
    private FileService fileService;
    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private GameTagRepository gameTagRepository;

    private GameEntity testGame;
    private TagEntity actionTag;
    private Long uniqueRawgId;

    @BeforeEach
    void setUp() {
        // Создаем уникальный ID, чтобы не было конфликта с существующими играми в вашей БД
        uniqueRawgId = (long) (Math.random() * 1000000);
        String uniqueName = "Test Game " + UUID.randomUUID().toString().substring(0, 5);

        // 1. Создаем тег (с уникальным именем)
        actionTag = new TagEntity();
        actionTag.setName("Action-" + uniqueRawgId);
        actionTag.setSlug("action-" + uniqueRawgId);
        actionTag = tagRepository.save(actionTag);

        // 2. Создаем игру
        testGame = new GameEntity();
        testGame.setName(uniqueName); // Уникальное имя
        testGame.setRawgId(uniqueRawgId);
        testGame.setRating(4.9);
        testGame.setReleaseDate(new Date());
        testGame = gameRepository.save(testGame);

        // 3. Создаем связь
        GameTag gameTag = new GameTag(testGame, actionTag);
        gameTagRepository.save(gameTag);
    }

    @Test
    void findByNameContainingIgnoreCase_ShouldReturnGame() {
        // Ищем именно то уникальное имя, которое создали
        Page<GameEntity> result = gameRepository.findByNameContainingIgnoreCase(testGame.getName(), PageRequest.of(0, 10));
        
        assertThat(result.getContent()).anyMatch(g -> g.getName().equals(testGame.getName()));
    }

    @Test
    void existsByRawgId_ShouldReturnTrue() {
        boolean exists = gameRepository.existsByRawgId(uniqueRawgId);
        assertThat(exists).isTrue();
    }

    @Test
    void findByTagId_ShouldReturnGameWithTag() {
        Page<GameEntity> result = gameRepository.findByTagId(actionTag.getId(), PageRequest.of(0, 10));
        
        // Проверяем, что среди результатов есть наша созданная игра
        assertThat(result.getContent()).extracting(GameEntity::getId).contains(testGame.getId());
    }

    @Test
    void findByTagIds_ShouldReturnDistinctGames() {
        TagEntity rpgTag = new TagEntity();
        rpgTag.setName("RPG-" + uniqueRawgId);
        rpgTag.setSlug("rpg-" + uniqueRawgId);
        rpgTag = tagRepository.save(rpgTag);

        gameTagRepository.save(new GameTag(testGame, rpgTag));

        List<Long> tagIds = List.of(actionTag.getId(), rpgTag.getId());
        Page<GameEntity> result = gameRepository.findByTagIds(tagIds, PageRequest.of(0, 10));

        // Проверяем, что наша игра есть в списке
        assertThat(result.getContent()).extracting(GameEntity::getId).contains(testGame.getId());
    }

    @Test
    void filterBySearch_ShouldWorkWithLike() {
        // Обрезаем имя для поиска по подстроке
        String searchPart = testGame.getName().substring(0, 8);
        Page<GameEntity> result = gameRepository.filterBySearch(searchPart, PageRequest.of(0, 10));
        
        assertThat(result.getContent()).extracting(GameEntity::getName).contains(testGame.getName());
    }
}