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
import java.time.LocalDateTime;
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
class ReviewReactionRepositoryTest {

    @Autowired
    private ReviewReactionRepository reviewReactionRepository;

    @Autowired
    private UserGameRepository userGamesRepository; // Репозиторий для отзывов

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @MockitoBean
    private GigachatService gigachatService;

    @MockitoBean
    private FileService fileService;

    private UserEntity user;
    private UserGames review;
    private ReactionType likeType = ReactionType.LIKE;
    private ReactionType dislikeType = ReactionType.DISLIKE;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setUsername("reviewer_" + UUID.randomUUID().toString().substring(0, 5));
        user.setEmail(UUID.randomUUID().toString().substring(0, 10) + "@test.com");
        user.setRegistrationDate(LocalDate.now());
        user.setPasswordHash("password");
        user = userRepository.save(user);

        GameEntity game = new GameEntity();
        game.setName("Game for Review");
        game.setRawgId((long) (Math.random() * 1000000));
        game = gameRepository.save(game);

        review = new UserGames();
        review.setUser(user);
        review.setGame(game);
        review.setReview("This is a test review");
        review.setInteraction(InteractionEnum.Review);
        review.setTime(LocalDateTime.now());
        review = userGamesRepository.save(review);
    }

    @Test
    void countByReviewIdAndType_ShouldReturnCorrectCount() {
        createReaction(user, review, likeType);
        
        UserEntity anotherUser = createAnotherUser();
        createReaction(anotherUser, review, likeType);

        long count = reviewReactionRepository.countByReviewIdAndType(review.getId(), likeType);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findByUserIdAndReviewId_ShouldReturnOptionalReaction() {
        createReaction(user, review, dislikeType);

        Optional<ReviewReaction> found = reviewReactionRepository.findByUserIdAndReviewId(user.getId(), review.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo(dislikeType);
    }

    @Test
    void findByReviewId_ShouldReturnAllReactionsForReview() {
        createReaction(user, review, likeType);
        UserEntity anotherUser = createAnotherUser();
        createReaction(anotherUser, review, dislikeType);
        List<ReviewReaction> result = reviewReactionRepository.findByReviewId(review.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    void getReactionCounts_ShouldReturnAggregatedCounts() {
        createReaction(user, review, likeType);
        createReaction(createAnotherUser(), review, likeType);
        createReaction(createAnotherUser(), review, dislikeType);

        List<Object[]> counts = reviewReactionRepository.getReactionCounts(review.getId());

        assertThat(counts).isNotEmpty();
        
        for (Object[] row : counts) {
            ReactionType type = (ReactionType) row[0];
            Long count = (Long) row[1];
            
            if (type == likeType) assertThat(count).isEqualTo(2L);
            if (type == dislikeType) assertThat(count).isEqualTo(1L);
        }
    }

    private void createReaction(UserEntity u, UserGames r, ReactionType t) {
        ReviewReaction reaction = new ReviewReaction();
        reaction.setUser(u);
        reaction.setReview(r);
        reaction.setType(t);
        reviewReactionRepository.save(reaction);
    }

    private UserEntity createAnotherUser() {
        UserEntity anotherUser = new UserEntity();
        anotherUser.setUsername("user_" + UUID.randomUUID().toString().substring(0, 8));
        anotherUser.setEmail(UUID.randomUUID().toString().substring(0, 10) + "@test.com");
        anotherUser.setRegistrationDate(LocalDate.now());
        anotherUser.setPasswordHash("password");
        return userRepository.save(anotherUser);
    }
}