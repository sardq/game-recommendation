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

import java.time.LocalDate;
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
class UserPreferenceRepositoryTest {

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @MockitoBean
    private GigachatService gigachatService;

    @MockitoBean
    private FileService fileService;

    private UserEntity testUser;
    private TagEntity actionTag;
    private TagEntity rpgTag;
    private String uniqueSuffix;

    @BeforeEach
    void setUp() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Создаем пользователя (email < 30 символов)
        testUser = new UserEntity();
        testUser.setUsername("user_" + uniqueSuffix);
        testUser.setRegistrationDate(LocalDate.now());
        testUser.setPasswordHash("password");
        testUser.setEmail(uniqueSuffix + "@t.com");
        testUser = userRepository.save(testUser);

        // 2. Создаем теги
        actionTag = new TagEntity();
        actionTag.setName("Action_" + uniqueSuffix);
        actionTag = tagRepository.save(actionTag);

        rpgTag = new TagEntity();
        rpgTag.setName("RPG_" + uniqueSuffix);
        rpgTag = tagRepository.save(rpgTag);

        // 3. Создаем предпочтения
        UserPreference pref1 = new UserPreference();
        pref1.setUser(testUser);
        pref1.setTag(actionTag);
        pref1.setPreferenceWeight(5.5);
        userPreferenceRepository.save(pref1);

        UserPreference pref2 = new UserPreference();
        pref2.setUser(testUser);
        pref2.setTag(rpgTag);
        pref2.setPreferenceWeight(10.0);
        userPreferenceRepository.save(pref2);
    }

    @Test
    void findByUserId_ShouldReturnAllPreferencesForUser() {
        List<UserPreference> result = userPreferenceRepository.findByUserId(testUser.getId());
        assertThat(result).hasSize(2);
    }

    @Test
    void findByUserIdOrderByPreferenceWeightDesc_ShouldSortCorrectly() {
        // Act
        List<UserPreference> result = userPreferenceRepository
                .findByUserIdOrderByPreferenceWeightDesc(testUser.getId());

        // Assert: RPG с весом 10.0 должен быть первым, Action с 5.5 - вторым
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTag().getName()).isEqualTo(rpgTag.getName());
        assertThat(result.get(0).getPreferenceWeight()).isEqualTo(10.0);
        assertThat(result.get(1).getTag().getName()).isEqualTo(actionTag.getName());
    }

    @Test
    void findByUserIdAndTagId_ShouldReturnSpecificPreference() {
        Optional<UserPreference> found = userPreferenceRepository
                .findByUserIdAndTagId(testUser.getId(), actionTag.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getPreferenceWeight()).isEqualTo(5.5);
    }

    @Test
    void existsByUserId_ShouldReturnTrueIfPreferencesExist() {
        assertThat(userPreferenceRepository.existsByUserId(testUser.getId())).isTrue();
        assertThat(userPreferenceRepository.existsByUserId(999L)).isFalse();
    }

    @Test
    void countByUserId_ShouldReturnCorrectCount() {
        assertThat(userPreferenceRepository.countByUserId(testUser.getId())).isEqualTo(2L);
    }

    @Test
    void deleteByUserId_ShouldRemoveAllUserPreferences() {
        // Act
        userPreferenceRepository.deleteByUserId(testUser.getId());
        userPreferenceRepository.flush(); // Принудительно применяем изменения

        // Assert
        assertThat(userPreferenceRepository.findByUserId(testUser.getId())).isEmpty();
        assertThat(userPreferenceRepository.existsByUserId(testUser.getId())).isFalse();
    }

    @Test
    void save_ShouldUpdateExistingPreferenceWeight() {
        // Arrange: Находим существующее предпочтение и меняем вес
        Optional<UserPreference> prefOpt = userPreferenceRepository
                .findByUserIdAndTagId(testUser.getId(), actionTag.getId());
        
        UserPreference pref = prefOpt.get();
        pref.setPreferenceWeight(20.0);

        // Act
        userPreferenceRepository.save(pref);
        userPreferenceRepository.flush();

        // Assert
        Optional<UserPreference> updated = userPreferenceRepository
                .findByUserIdAndTagId(testUser.getId(), actionTag.getId());
        assertThat(updated.get().getPreferenceWeight()).isEqualTo(20.0);
    }
}