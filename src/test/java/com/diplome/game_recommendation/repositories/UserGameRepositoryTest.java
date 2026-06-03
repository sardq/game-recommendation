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
import java.time.LocalDateTime;
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
class UserGameRepositoryTest {

    @Autowired
    private UserGameRepository userGameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private GameTagRepository gameTagRepository;

    @MockitoBean
    private GigachatService gigachatService;

    @MockitoBean
    private FileService fileService;

    private UserEntity testUser;
    private GameEntity testGame;
    private String uniqueSuffix;

    @BeforeEach
    void setUp() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Создаем пользователя (email < 30 символов)
        testUser = new UserEntity();
        testUser.setUsername("user_" + uniqueSuffix);
        testUser.setEmail(uniqueSuffix + "@t.com");
        testUser.setRegistrationDate(LocalDate.now());
        testUser.setPasswordHash("password");
        testUser = userRepository.save(testUser);

        // 2. Создаем игру
        testGame = new GameEntity();
        testGame.setName("The Witcher " + uniqueSuffix);
        testGame.setRawgId((long) (Math.random() * 1000000));
        testGame = gameRepository.save(testGame);
    }

    @Test
    void getAverageRatingForGame_ShouldReturnCorrectAverage() {
        UserGames rating1 = new UserGames();
        rating1.setUser(testUser);
        rating1.setGame(testGame);
        rating1.setInteraction(InteractionEnum.Rated);
        rating1.setRating(4);
        userGameRepository.save(rating1);

        UserEntity user2 = createAnotherUser();
        UserGames rating2 = new UserGames();
        rating2.setUser(user2);
        rating2.setGame(testGame);
        rating2.setInteraction(InteractionEnum.Rated);
        rating2.setRating(5);
        userGameRepository.save(rating2);

        
        Double avg = userGameRepository.getAverageRatingForGame(testGame.getId());

        
        assertThat(avg).isEqualTo(4.5);
    }

    @Test
    void getCountOfRatingsForGame_ShouldReturnCorrectCount() {
        
        UserGames rating = new UserGames();
        rating.setUser(testUser);
        rating.setGame(testGame);
        rating.setInteraction(InteractionEnum.Rated);
        rating.setRating(5);
        userGameRepository.save(rating);

        
        Integer count = userGameRepository.getCountOfRatingsForGame(testGame.getId());

        
        assertThat(count).isEqualTo(1);
    }

    @Test
    void findByGameIdAndReviewIsNotNullOrderByTimeDesc_ShouldReturnReviewsOnly() {
        UserGames review = new UserGames();
        review.setUser(testUser);
        review.setGame(testGame);
        review.setInteraction(InteractionEnum.Review);
        review.setReview("Great game!");
        review.setTime(LocalDateTime.now());
        userGameRepository.save(review);

        UserGames justRating = new UserGames();
        justRating.setUser(createAnotherUser());
        justRating.setGame(testGame);
        justRating.setInteraction(InteractionEnum.Rated);
        justRating.setRating(5);
        userGameRepository.save(justRating);

        
        Page<UserGames> result = userGameRepository.findByGameIdAndReviewIsNotNullOrderByTimeDesc(
                testGame.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReview()).isEqualTo("Great game!");
    }

    @Test
    void findFavoritesFiltered_ShouldFilterByNameAndTag() {
        
        TagEntity rpgTag = new TagEntity();
        rpgTag.setName("RPG_" + uniqueSuffix);
        rpgTag = tagRepository.save(rpgTag);
        gameTagRepository.save(new GameTag(testGame, rpgTag));

        UserGames favorite = new UserGames();
        favorite.setUser(testUser);
        favorite.setGame(testGame);
        favorite.setInteraction(InteractionEnum.Favorite);
        userGameRepository.save(favorite);

        Page<GameEntity> searchResult = userGameRepository.findFavoritesFiltered(
                testUser.getId(), "Witcher", null, PageRequest.of(0, 10));
        assertThat(searchResult.getContent()).extracting(GameEntity::getId).contains(testGame.getId());

        Page<GameEntity> tagResult = userGameRepository.findFavoritesFiltered(
                testUser.getId(), null, rpgTag.getId(), PageRequest.of(0, 10));
        assertThat(tagResult.getContent()).hasSize(1);

        Page<GameEntity> emptyResult = userGameRepository.findFavoritesFiltered(
                testUser.getId(), null, 999L, PageRequest.of(0, 10));
        assertThat(emptyResult.getContent()).isEmpty();
    }

    @Test
    void findByUserIdAndInteraction_ShouldReturnInteractions() {
        
        UserGames interaction = new UserGames();
        interaction.setUser(testUser);
        interaction.setGame(testGame);
        interaction.setInteraction(InteractionEnum.Favorite);
        userGameRepository.save(interaction);

        
        List<UserGames> result = userGameRepository.findByUserIdAndInteraction(testUser.getId(), InteractionEnum.Favorite);

        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInteraction()).isEqualTo(InteractionEnum.Favorite);
    }

    @Test
    void findByUserIdAndGameIdAndInteraction_ShouldReturnOptional() {
        
        UserGames review = new UserGames();
        review.setUser(testUser);
        review.setGame(testGame);
        review.setInteraction(InteractionEnum.Review);
        userGameRepository.save(review);

        
        Optional<UserGames> found = userGameRepository.findByUserIdAndGameIdAndInteraction(
                testUser.getId(), testGame.getId(), InteractionEnum.Review);

        
        assertThat(found).isPresent();
    }

    // Вспомогательный метод
    private UserEntity createAnotherUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserEntity user = new UserEntity();
        user.setUsername("u_" + suffix);
        user.setEmail(suffix + "@t.com");
        user.setRegistrationDate(LocalDate.now());
        user.setPasswordHash("password");
        return userRepository.save(user);
    }
}