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

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameTagRepository gameTagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository; // Предполагаем наличие этого репозитория

    @MockitoBean
    private GigachatService gigachatService;

    @MockitoBean
    private FileService fileService;

    private TagEntity actionTag;
    private TagEntity rpgTag;
    private GameEntity testGame;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        actionTag = new TagEntity();
        actionTag.setName("Action_" + unique);
        actionTag.setNameRu("Экшен_" + unique);
        actionTag.setKeep(true);
        actionTag = tagRepository.save(actionTag);

        rpgTag = new TagEntity();
        rpgTag.setName("RPG_" + unique);
        rpgTag.setKeep(false); // Для проверки фильтра по keep
        rpgTag = tagRepository.save(rpgTag);

        testGame = new GameEntity();
        testGame.setName("Test Game " + unique);
        testGame.setRawgId((long) (Math.random() * 1000000));
        testGame = gameRepository.save(testGame);

        gameTagRepository.save(new GameTag(testGame, actionTag));
    }

    @Test
    void findByName_ShouldReturnCorrectTag() {
        Optional<TagEntity> found = tagRepository.findByName(actionTag.getName());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(actionTag.getId());
    }


    @Test
    void findTagsByGameId_ShouldReturnAllTagsForGame() {
        List<TagEntity> tags = tagRepository.findTagsByGameId(testGame.getId());
        assertThat(tags).hasSize(1);
        assertThat(tags.get(0).getName()).isEqualTo(actionTag.getName());
    }

    @Test
    void findByKeep_ShouldFilterByStatus() {
        List<TagEntity> keptTags = tagRepository.findByKeep(true);
        assertThat(keptTags).extracting(TagEntity::getName).contains(actionTag.getName());
        assertThat(keptTags).extracting(TagEntity::getName).doesNotContain(rpgTag.getName());
    }

    @Test
    void filterBySearch_ShouldFindByNameOrNameRu() {
        Page<TagEntity> resultEn = tagRepository.filterBySearch("Action", PageRequest.of(0, 10));
        assertThat(resultEn.getContent()).extracting(TagEntity::getName).contains(actionTag.getName());

        Page<TagEntity> resultRu = tagRepository.filterBySearch("экшен", PageRequest.of(0, 10));
        assertThat(resultRu.getContent()).extracting(TagEntity::getNameRu).contains(actionTag.getNameRu());
    }

    @Test
    void getTagsSortedByPreference_ShouldOrderByWeight() {
        UserEntity user = new UserEntity();
        String userUnique = UUID.randomUUID().toString().substring(0, 8);
        user.setUsername("u_" + userUnique);
        user.setEmail(userUnique + "@t.com");
        user.setRegistrationDate(LocalDate.now());
        user.setPasswordHash("password");
        user = userRepository.save(user);

        UserPreference prefAction = new UserPreference();
        prefAction.setUser(user);
        prefAction.setTag(actionTag);
        prefAction.setPreferenceWeight(5.0);
        userPreferenceRepository.save(prefAction);

        UserPreference prefRpg = new UserPreference();
        prefRpg.setUser(user);
        prefRpg.setTag(rpgTag);
        prefRpg.setPreferenceWeight(10.0);
        userPreferenceRepository.save(prefRpg);

        Page<TagEntity> result = tagRepository.getTagsSortedByPreference(user.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo(rpgTag.getName());
        assertThat(result.getContent().get(1).getName()).isEqualTo(actionTag.getName());
    }
}