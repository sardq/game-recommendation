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
class GameTagRepositoryTest {

    @Autowired
    private GameTagRepository gameTagRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private TagRepository tagRepository;

    @MockitoBean
    private GigachatService gigachatService;

    @MockitoBean
    private FileService fileService;

    private GameEntity testGame;
    private TagEntity testTag;
    private Long uniqueId;

    @BeforeEach
    void setUp() {
        uniqueId = (long) (Math.random() * 10000000);
        
        testTag = new TagEntity();
        testTag.setName("Tag-" + uniqueId);
        testTag.setSlug("slug-" + uniqueId);
        testTag = tagRepository.save(testTag);

        testGame = new GameEntity();
        testGame.setName("Game-" + UUID.randomUUID().toString().substring(0, 5));
        testGame.setRawgId(uniqueId);
        testGame.setReleaseDate(new Date());
        testGame = gameRepository.save(testGame);

        GameTag gameTag = new GameTag(testGame, testTag);
        gameTagRepository.save(gameTag);
    }

    @Test
    void findByGameId_ShouldReturnListOfTagsForGame() {
        
        List<GameTag> result = gameTagRepository.findByGameId(testGame.getId());

        
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getGame().getId()).isEqualTo(testGame.getId());
        assertThat(result.get(0).getTag().getId()).isEqualTo(testTag.getId());
    }

    @Test
    void findByTagId_ShouldReturnListOfGamesForTag() {
        
        List<GameTag> result = gameTagRepository.findByTagId(testTag.getId());

        
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getTag().getId()).isEqualTo(testTag.getId());
        assertThat(result.get(0).getGame().getId()).isEqualTo(testGame.getId());
    }

    @Test
    void existsByGameIdAndTagId_ShouldReturnTrueForExistingLink() {
        
        boolean exists = gameTagRepository.existsByGameIdAndTagId(testGame.getId(), testTag.getId());

        
        assertThat(exists).isTrue();
    }

    @Test
    void existsByGameIdAndTagId_ShouldReturnFalseForNonExistentLink() {
        
        boolean exists = gameTagRepository.existsByGameIdAndTagId(testGame.getId(), 999999L);

        
        assertThat(exists).isFalse();
    }

    @Test
    void findByGameId_WithMultipleTags_ShouldReturnAllAssociatedTags() {
        TagEntity secondTag = new TagEntity();
        secondTag.setName("SecondTag-" + uniqueId);
        secondTag.setSlug("second-slug-" + uniqueId);
        secondTag = tagRepository.save(secondTag);

        GameTag secondLink = new GameTag(testGame, secondTag);
        gameTagRepository.save(secondLink);

        
        List<GameTag> result = gameTagRepository.findByGameId(testGame.getId());

        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(gt -> gt.getTag().getName())
                .containsExactlyInAnyOrder(testTag.getName(), secondTag.getName());
    }
}