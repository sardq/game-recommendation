package com.diplome.game_recommendation.services;

import com.diplome.game_recommendation.dtos.GameDto;
import com.diplome.game_recommendation.dtos.ReviewDto;
import com.diplome.game_recommendation.models.*;
import com.diplome.game_recommendation.repositories.GameRepository;
import com.diplome.game_recommendation.repositories.ReviewReactionRepository;
import com.diplome.game_recommendation.repositories.UserGameRepository;
import com.diplome.game_recommendation.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserGameRepository userGamesRepository;

    @Mock
    private ReviewReactionRepository reactionRepository;

    @Mock
    private GameService gameService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InteractionService interactionService;

    private UserEntity testUser;
    private GameEntity testGame;
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_GAME_ID = 100L;
    private final Long TEST_REVIEW_ID = 500L;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail(TEST_EMAIL);
        testUser.setUsername(TEST_USERNAME);
        testUser.setAvatarUrl("http://test.com/avatar.jpg");

        testGame = new GameEntity();
        testGame.setId(TEST_GAME_ID);
        testGame.setName("Test Game");
        testGame.setRawgId(12345L);
        testGame.setPosterUrl("http://test.com/poster.jpg");

        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
    }

    @Test
    void recordView_ShouldSaveViewInteraction() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        
        ArgumentCaptor<UserGames> captor = ArgumentCaptor.forClass(UserGames.class);

        // Act
        interactionService.recordView(authentication, TEST_GAME_ID);

        // Assert
        verify(userGamesRepository).save(captor.capture());
        UserGames savedInteraction = captor.getValue();
        
        assertEquals(testUser, savedInteraction.getUser());
        assertEquals(testGame, savedInteraction.getGame());
        assertEquals(InteractionEnum.Viewed, savedInteraction.getInteraction());
        assertNotNull(savedInteraction.getTime());
    }

    @Test
    void recordView_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, 
            () -> interactionService.recordView(authentication, TEST_GAME_ID));
        
        verify(userGamesRepository, never()).save(any());
    }

    @Test
    void recordView_WhenGameNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, 
            () -> interactionService.recordView(authentication, TEST_GAME_ID));
        
        verify(userGamesRepository, never()).save(any());
    }

    @Test
    void getUserReview_WhenReviewAndRatingExist_ShouldReturnDto() {
        // Arrange
        UserGames review = new UserGames();
        review.setReview("Great game!");
        
        UserGames rating = new UserGames();
        rating.setRating(5);

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Review))
            .thenReturn(Optional.of(review));
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Rated))
            .thenReturn(Optional.of(rating));

        // Act
        ReviewDto result = interactionService.getUserReview(TEST_GAME_ID, authentication);

        // Assert
        assertNotNull(result);
        assertEquals("Great game!", result.getReview());
        assertEquals(5, result.getRating());
    }

    @Test
    void getUserReview_WhenNoReviewAndNoRating_ShouldReturnEmptyDto() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Review))
            .thenReturn(Optional.empty());
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Rated))
            .thenReturn(Optional.empty());

        // Act
        ReviewDto result = interactionService.getUserReview(TEST_GAME_ID, authentication);

        // Assert
        assertNotNull(result);
        assertNull(result.getReview());
        assertNull(result.getRating());
    }

    @Test
    void getReviewsByUserIdPaginated_ShouldReturnReviewsWithRatings() {
        // Arrange
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "time"));
        
        UserGames review1 = createUserGamesWithReview(testUser, testGame, "Good game", LocalDateTime.now());
        UserGames review2 = createUserGamesWithReview(testUser, testGame, "Awesome!", LocalDateTime.now().minusDays(1));
        
        List<UserGames> reviews = Arrays.asList(review1, review2);
        Page<UserGames> reviewPage = new PageImpl<>(reviews, pageable, reviews.size());
        
        UserGames rating1 = createUserGamesWithRating(testUser, testGame, 4);
        
        when(userGamesRepository.findByUserIdAndInteraction(eq(TEST_USER_ID), eq(InteractionEnum.Review), eq(pageable)))
            .thenReturn(reviewPage);
        when(userGamesRepository.findByUserIdAndInteraction(eq(TEST_USER_ID), eq(InteractionEnum.Rated)))
            .thenReturn(Arrays.asList(rating1));

        // Act
        List<ReviewDto> result = interactionService.getReviewsByUserIdPaginated(TEST_USER_ID, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Good game", result.get(0).getReview());
        assertEquals(4, result.get(0).getRating());
        assertEquals(TEST_USERNAME, result.get(0).getLogin());
        assertEquals("Test Game", result.get(0).getGameTitile());
    }

    @Test
    void getReviewsByUserIdPaginated_WhenNoReviews_ShouldReturnEmptyList() {
        // Arrange
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "time"));
        
        Page<UserGames> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
        
        when(userGamesRepository.findByUserIdAndInteraction(eq(TEST_USER_ID), eq(InteractionEnum.Review), eq(pageable)))
            .thenReturn(emptyPage);

        // Act
        List<ReviewDto> result = interactionService.getReviewsByUserIdPaginated(TEST_USER_ID, page, size);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userGamesRepository, never()).findByUserIdAndInteraction(eq(TEST_USER_ID), eq(InteractionEnum.Rated));
    }

    @Test
    void recordRating_ShouldSaveRatingAndUpdateLocalRating() {
        // Arrange
        Integer rating = 5;
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Rated))
            .thenReturn(Optional.empty());
        
        ArgumentCaptor<UserGames> captor = ArgumentCaptor.forClass(UserGames.class);

        // Act
        interactionService.recordRating(authentication, TEST_GAME_ID, rating);

        // Assert
        verify(userGamesRepository).saveAndFlush(captor.capture());
        UserGames savedRating = captor.getValue();
        
        assertEquals(testUser, savedRating.getUser());
        assertEquals(testGame, savedRating.getGame());
        assertEquals(InteractionEnum.Rated, savedRating.getInteraction());
        assertEquals(rating, savedRating.getRating());
        assertNotNull(savedRating.getTime());
        
        verify(gameService).updateLocalRating(TEST_GAME_ID);
    }

    @Test
    void recordRating_WhenRatingExists_ShouldUpdateExisting() {
        // Arrange
        Integer newRating = 4;
        UserGames existingRating = new UserGames();
        existingRating.setRating(3);
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Rated))
            .thenReturn(Optional.of(existingRating));
        
        // Act
        interactionService.recordRating(authentication, TEST_GAME_ID, newRating);

        // Assert
        verify(userGamesRepository).saveAndFlush(existingRating);
        assertEquals(newRating, existingRating.getRating());
        assertNotNull(existingRating.getTime());
    }

    @Test
    void addToFavorites_ShouldSaveFavorite() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        
        ArgumentCaptor<UserGames> captor = ArgumentCaptor.forClass(UserGames.class);

        // Act
        interactionService.addToFavorites(authentication, TEST_GAME_ID);

        // Assert
        verify(userGamesRepository).save(captor.capture());
        UserGames favorite = captor.getValue();
        
        assertEquals(testUser, favorite.getUser());
        assertEquals(testGame, favorite.getGame());
        assertEquals(InteractionEnum.Favorite, favorite.getInteraction());
        assertNotNull(favorite.getTime());
    }

    @Test
    void removeFromFavorites_ShouldDeleteFavorite() {
        // Arrange
        UserGames favorite = new UserGames();
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Favorite))
            .thenReturn(Optional.of(favorite));

        // Act
        interactionService.removeFromFavorites(authentication, TEST_GAME_ID);

        // Assert
        verify(userGamesRepository).delete(favorite);
    }

    @Test
    void removeFromFavorites_WhenNotFavorite_ShouldDoNothing() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Favorite))
            .thenReturn(Optional.empty());

        // Act
        interactionService.removeFromFavorites(authentication, TEST_GAME_ID);

        // Assert
        verify(userGamesRepository, never()).delete(any());
    }

    @Test
    void isFavorite_WhenFavoriteExists_ShouldReturnTrue() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Favorite))
            .thenReturn(Optional.of(new UserGames()));

        // Act
        boolean result = interactionService.isFavorite(authentication, TEST_GAME_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    void isFavorite_WhenFavoriteNotExists_ShouldReturnFalse() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Favorite))
            .thenReturn(Optional.empty());

        // Act
        boolean result = interactionService.isFavorite(authentication, TEST_GAME_ID);

        // Assert
        assertFalse(result);
    }

    @Test
    void getReviewsByGame_ShouldReturnReviewsWithReactions() {
        // Arrange
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        UserGames review = createUserGamesWithReview(testUser, testGame, "Excellent game!", LocalDateTime.now());
        review.setId(TEST_REVIEW_ID);
        
        UserGames rating = createUserGamesWithRating(testUser, testGame, 5);
        
        List<UserGames> reviews = Collections.singletonList(review);
        Page<UserGames> reviewPage = new PageImpl<>(reviews, pageable, reviews.size());
        
        List<UserGames> ratings = Collections.singletonList(rating);
        
        ReviewReaction likeReaction = new ReviewReaction();
        likeReaction.setType(ReactionType.LIKE);
        likeReaction.setUser(testUser);
        
        ReviewReaction funnyReaction = new ReviewReaction();
        funnyReaction.setType(ReactionType.FUNNY);
        
        List<ReviewReaction> reactions = Arrays.asList(likeReaction, funnyReaction);
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findByGameIdAndReviewIsNotNullOrderByTimeDesc(eq(TEST_GAME_ID), eq(pageable)))
            .thenReturn(reviewPage);
        when(userGamesRepository.findByGameIdAndRatingIsNotNullOrderByTimeDesc(eq(TEST_GAME_ID), eq(pageable)))
            .thenReturn(new PageImpl<>(ratings));
        when(reactionRepository.findByReviewId(TEST_REVIEW_ID)).thenReturn(reactions);

        // Act
        List<ReviewDto> result = interactionService.getReviewsByGame(TEST_GAME_ID, page, size, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        ReviewDto dto = result.get(0);
        assertEquals("Excellent game!", dto.getReview());
        assertEquals(5, dto.getRating());
        assertEquals(TEST_USERNAME, dto.getLogin());
        assertEquals(1, dto.getLikesCount());
        assertEquals(0, dto.getDislikesCount());
        assertEquals(1, dto.getFunnyCount());
        assertEquals(ReactionType.LIKE.name(), dto.getCurrentUserReaction());
    }

    @Test
    void getReviewsByGame_WhenNoAuthentication_ShouldReturnReviewsWithoutUserReaction() {
        // Arrange
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        UserGames review = createUserGamesWithReview(testUser, testGame, "Great!", LocalDateTime.now());
        review.setId(TEST_REVIEW_ID);
        
        List<UserGames> reviews = Collections.singletonList(review);
        Page<UserGames> reviewPage = new PageImpl<>(reviews, pageable, reviews.size());
        
        when(userGamesRepository.findByGameIdAndReviewIsNotNullOrderByTimeDesc(eq(TEST_GAME_ID), eq(pageable)))
            .thenReturn(reviewPage);
        when(userGamesRepository.findByGameIdAndRatingIsNotNullOrderByTimeDesc(eq(TEST_GAME_ID), eq(pageable)))
            .thenReturn(new PageImpl<>(new ArrayList<>()));
        when(reactionRepository.findByReviewId(TEST_REVIEW_ID)).thenReturn(new ArrayList<>());

        // Act
        List<ReviewDto> result = interactionService.getReviewsByGame(TEST_GAME_ID, page, size, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getCurrentUserReaction());
    }

    @Test
    void addReview_ShouldSaveReview() {
        // Arrange
        String reviewText = "This is a great game!";
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Review))
            .thenReturn(Optional.empty());
        
        ArgumentCaptor<UserGames> captor = ArgumentCaptor.forClass(UserGames.class);

        // Act
        interactionService.addReview(authentication, TEST_GAME_ID, reviewText);

        // Assert
        verify(userGamesRepository).save(captor.capture());
        UserGames savedReview = captor.getValue();
        
        assertEquals(testUser, savedReview.getUser());
        assertEquals(testGame, savedReview.getGame());
        assertEquals(InteractionEnum.Review, savedReview.getInteraction());
        assertEquals(reviewText, savedReview.getReview());
        assertNotNull(savedReview.getTime());
    }

    @Test
    void addReview_WhenReviewExists_ShouldUpdateExisting() {
        // Arrange
        String newReviewText = "Updated review";
        UserGames existingReview = new UserGames();
        existingReview.setReview("Old review");
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Review))
            .thenReturn(Optional.of(existingReview));

        // Act
        interactionService.addReview(authentication, TEST_GAME_ID, newReviewText);

        // Assert
        verify(userGamesRepository).save(existingReview);
        assertEquals(newReviewText, existingReview.getReview());
        assertNotNull(existingReview.getTime());
    }

    @Test
    void getUserFavorites_ShouldReturnLast5Favorites() {
        // Arrange
        List<UserGames> favorites = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            GameEntity game = new GameEntity();
            game.setId((long) i);
            game.setName("Game " + i);
            game.setPosterUrl("http://test.com/game" + i + ".jpg");
            
            UserGames fav = new UserGames();
            fav.setGame(game);
            favorites.add(fav);
        }
        
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "time"));
        Page<UserGames> favoritesPage = new PageImpl<>(favorites, pageable, favorites.size());
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findByUserIdAndInteraction(eq(TEST_USER_ID), eq(InteractionEnum.Favorite), eq(pageable)))
            .thenReturn(favoritesPage);

        // Act
        List<GameDto> result = interactionService.getUserFavorites(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("Game 1", result.get(0).getName());
        assertNotNull(result.get(0).getPosterUrl());
    }

    @Test
    void getUserReviews_ShouldReturnUserReviewsWithRatings() {
        // Arrange
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "time"));
        
        UserGames review1 = createUserGamesWithReview(testUser, testGame, "Nice game", LocalDateTime.now());
        
        List<UserGames> reviews = Collections.singletonList(review1);
        Page<UserGames> reviewPage = new PageImpl<>(reviews, pageable, reviews.size());
        
        UserGames rating1 = createUserGamesWithRating(testUser, testGame, 4);
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findByUserIdAndInteraction(eq(TEST_USER_ID), eq(InteractionEnum.Review), eq(pageable)))
            .thenReturn(reviewPage);
        when(userGamesRepository.findByUserIdAndInteraction(eq(TEST_USER_ID), eq(InteractionEnum.Rated)))
            .thenReturn(Collections.singletonList(rating1));

        // Act
        List<ReviewDto> result = interactionService.getUserReviews(authentication, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Nice game", result.get(0).getReview());
        assertEquals(4, result.get(0).getRating());
        assertEquals(TEST_USERNAME, result.get(0).getLogin());
        assertEquals("Test Game", result.get(0).getGameTitile());
    }

    @Test
    void reactToReview_WhenNoExistingReaction_ShouldCreateNew() {
        // Arrange
        String reactionType = "LIKE";
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(reactionRepository.findByUserIdAndReviewId(TEST_USER_ID, TEST_REVIEW_ID))
            .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(TEST_USER_ID)).thenReturn(testUser);
        when(userGamesRepository.getReferenceById(TEST_REVIEW_ID)).thenReturn(new UserGames());
        
        ArgumentCaptor<ReviewReaction> captor = ArgumentCaptor.forClass(ReviewReaction.class);

        // Act
        interactionService.reactToReview(authentication, TEST_REVIEW_ID, reactionType);

        // Assert
        verify(reactionRepository).save(captor.capture());
        ReviewReaction savedReaction = captor.getValue();
        
        assertEquals(ReactionType.LIKE, savedReaction.getType());
        assertEquals(testUser, savedReaction.getUser());
        assertNotNull(savedReaction.getReview());
    }

    @Test
    void reactToReview_WhenSameReactionExists_ShouldDelete() {
        // Arrange
        String reactionType = "LIKE";
        ReviewReaction existingReaction = new ReviewReaction();
        existingReaction.setType(ReactionType.LIKE);
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(reactionRepository.findByUserIdAndReviewId(TEST_USER_ID, TEST_REVIEW_ID))
            .thenReturn(Optional.of(existingReaction));

        // Act
        interactionService.reactToReview(authentication, TEST_REVIEW_ID, reactionType);

        // Assert
        verify(reactionRepository).delete(existingReaction);
        verify(reactionRepository, never()).save(any());
    }

    @Test
    void reactToReview_WhenDifferentReactionExists_ShouldUpdate() {
        // Arrange
        String reactionType = "LIKE";
        ReviewReaction existingReaction = new ReviewReaction();
        existingReaction.setType(ReactionType.DISLIKE);
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(reactionRepository.findByUserIdAndReviewId(TEST_USER_ID, TEST_REVIEW_ID))
            .thenReturn(Optional.of(existingReaction));

        // Act
        interactionService.reactToReview(authentication, TEST_REVIEW_ID, reactionType);

        // Assert
        verify(reactionRepository).save(existingReaction);
        assertEquals(ReactionType.LIKE, existingReaction.getType());
        verify(reactionRepository, never()).delete(any());
    }

    @Test
    void getFavoritesFiltered_ShouldReturnFilteredFavorites() {
        // Arrange
        String search = "Game";
        Long tagId = 10L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<GameEntity> expectedPage = new PageImpl<>(Collections.singletonList(testGame), pageable, 1);
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findFavoritesFiltered(TEST_USER_ID, search, tagId, pageable))
            .thenReturn(expectedPage);

        // Act
        Page<GameEntity> result = interactionService.getFavoritesFiltered(authentication, search, tagId, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testGame, result.getContent().get(0));
    }

    @Test
    void getReviewsByUserId_ShouldReturnAllUserReviews() {
        // Arrange
        UserGames review1 = createUserGamesWithReview(testUser, testGame, "Great!", LocalDateTime.now());
        UserGames review2 = createUserGamesWithReview(testUser, testGame, "Awesome!", LocalDateTime.now());
        
        List<UserGames> reviews = Arrays.asList(review1, review2);
        
        UserGames rating1 = createUserGamesWithRating(testUser, testGame, 5);
        
        when(userGamesRepository.findByUserIdAndInteraction(TEST_USER_ID, InteractionEnum.Review))
            .thenReturn(reviews);
        when(userGamesRepository.findByUserIdAndInteraction(TEST_USER_ID, InteractionEnum.Rated))
            .thenReturn(Collections.singletonList(rating1));

        // Act
        List<ReviewDto> result = interactionService.getReviewsByUserId(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Great!", result.get(0).getReview());
        assertEquals(5, result.get(0).getRating());
        assertEquals("Test Game", result.get(0).getGameTitile());
    }

    @Test
    void getReviewsByUserId_WhenNoRatings_ShouldHandleGracefully() {
        // Arrange
        UserGames review = createUserGamesWithReview(testUser, testGame, "Nice!", LocalDateTime.now());
        
        when(userGamesRepository.findByUserIdAndInteraction(TEST_USER_ID, InteractionEnum.Review))
            .thenReturn(Collections.singletonList(review));
        when(userGamesRepository.findByUserIdAndInteraction(TEST_USER_ID, InteractionEnum.Rated))
            .thenReturn(new ArrayList<>());

        // Act
        List<ReviewDto> result = interactionService.getReviewsByUserId(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getRating());
    }

    @Test
    void getUserInteraction_ShouldReturnInteraction() {
        // Arrange
        UserGames interaction = new UserGames();
        interaction.setInteraction(InteractionEnum.Favorite);
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findByUserIdAndGameIdAndInteraction(TEST_USER_ID, TEST_GAME_ID, InteractionEnum.Favorite))
            .thenReturn(Optional.of(interaction));

        // Act
        Optional<UserGames> result = interactionService.getUserInteraction(authentication, TEST_GAME_ID, "Favorite");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(InteractionEnum.Favorite, result.get().getInteraction());
    }

    // Helper methods
    private UserGames createUserGamesWithReview(UserEntity user, GameEntity game, String review, LocalDateTime time) {
        UserGames ug = new UserGames();
        ug.setUser(user);
        ug.setGame(game);
        ug.setInteraction(InteractionEnum.Review);
        ug.setReview(review);
        ug.setTime(time);
        return ug;
    }

    private UserGames createUserGamesWithRating(UserEntity user, GameEntity game, Integer rating) {
        UserGames ug = new UserGames();
        ug.setUser(user);
        ug.setGame(game);
        ug.setInteraction(InteractionEnum.Rated);
        ug.setRating(rating);
        ug.setTime(LocalDateTime.now());
        return ug;
    }
}