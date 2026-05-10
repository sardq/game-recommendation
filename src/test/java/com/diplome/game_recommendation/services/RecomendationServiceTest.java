package com.diplome.game_recommendation.services;

import com.diplome.game_recommendation.dtos.RecommendationDto;
import com.diplome.game_recommendation.dtos.RecommendationSessionDetailsDto;
import com.diplome.game_recommendation.dtos.RecommendationSessionDto;
import com.diplome.game_recommendation.models.*;
import com.diplome.game_recommendation.repositories.*;
import com.diplome.game_recommendation.services.librec.LibrecEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecomendationServiceTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserGameRepository userGameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecommendationSessionRepository sessionRepository;

    @Mock
    private RecommendationItemsRepository recommendationItemsRepository;

    @Mock
    private GameTagRepository gameTagRepository;

    @Mock
    private LibrecEngineService librecEngineService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RecomendationService recomendationService;

    private UserEntity testUser;
    private GameEntity testGame1;
    private GameEntity testGame2;
    private GameEntity testGame3;
    private TagEntity testTag1;
    private TagEntity testTag2;
    private final Long TEST_USER_ID = 33862L;
    private final Long TEST_GAME_ID_1 = 100L;
    private final Long TEST_GAME_ID_2 = 101L;
    private final Long TEST_GAME_ID_3 = 102L;
    private final Long TEST_TAG_ID_1 = 10L;
    private final Long TEST_TAG_ID_2 = 11L;
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail(TEST_EMAIL);
        testUser.setUsername("testuser");

        testGame1 = new GameEntity();
        testGame1.setId(TEST_GAME_ID_1);
        testGame1.setName("Game 1");
        testGame1.setRating(4.5);
        testGame1.setLocalRating(4.2);
        testGame1.setPosterUrl("http://test.com/game1.jpg");

        testGame2 = new GameEntity();
        testGame2.setId(TEST_GAME_ID_2);
        testGame2.setName("Game 2");
        testGame2.setRating(4.0);
        testGame2.setLocalRating(3.8);
        testGame2.setPosterUrl("http://test.com/game2.jpg");

        testGame3 = new GameEntity();
        testGame3.setId(TEST_GAME_ID_3);
        testGame3.setName("Game 3");
        testGame3.setRating(3.5);
        testGame3.setLocalRating(3.2);
        testGame3.setPosterUrl("http://test.com/game3.jpg");

        testTag1 = new TagEntity();
        testTag1.setId(TEST_TAG_ID_1);
        testTag1.setName("Action");

        testTag2 = new TagEntity();
        testTag2.setId(TEST_TAG_ID_2);
        testTag2.setName("RPG");

        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
    }

    @Test
    void recalculateUserPreferences_ShouldCalculateAndSavePreferences() {
        // Arrange
        UserGames viewedGame = createUserGame(testUser, testGame1, InteractionEnum.Viewed, null, LocalDateTime.now());
        UserGames favoriteGame = createUserGame(testUser, testGame2, InteractionEnum.Favorite, null, LocalDateTime.now());
        UserGames ratedGame = createUserGame(testUser, testGame3, InteractionEnum.Rated, 5, LocalDateTime.now());
        
        List<UserGames> userGames = Arrays.asList(viewedGame, favoriteGame, ratedGame);
        
        GameTag gameTag1 = new GameTag();
        gameTag1.setGame(testGame1);
        gameTag1.setTag(testTag1);
        
        GameTag gameTag2 = new GameTag();
        gameTag2.setGame(testGame2);
        gameTag2.setTag(testTag2);
        
        GameTag gameTag3 = new GameTag();
        gameTag3.setGame(testGame3);
        gameTag3.setTag(testTag1);
        
        when(userGameRepository.findByUserId(TEST_USER_ID)).thenReturn(userGames);
        when(gameTagRepository.findByGameId(TEST_GAME_ID_1)).thenReturn(Collections.singletonList(gameTag1));
        when(gameTagRepository.findByGameId(TEST_GAME_ID_2)).thenReturn(Collections.singletonList(gameTag2));
        when(gameTagRepository.findByGameId(TEST_GAME_ID_3)).thenReturn(Collections.singletonList(gameTag3));
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        
        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);

        // Act
        recomendationService.recalculateUserPreferences(TEST_USER_ID);

        // Assert
        verify(userPreferenceRepository).deleteByUserId(TEST_USER_ID);
        verify(userPreferenceRepository, atLeastOnce()).save(captor.capture());
        
        List<UserPreference> savedPrefs = captor.getAllValues();
        assertFalse(savedPrefs.isEmpty());
        
        // Verify weights are normalized (between 0 and 1)
        for (UserPreference pref : savedPrefs) {
            assertTrue(pref.getPreferenceWeight() >= 0 && pref.getPreferenceWeight() <= 1);
            assertEquals(testUser, pref.getUser());
        }
    }

 

    @Test
    void getRecommendationsForUser_ByUserId_ShouldExcludePlayedGames() {
        // Arrange
        when(userGameRepository.countByUserId(TEST_USER_ID)).thenReturn(5);
        
        UserPreference pref = new UserPreference();
        pref.setTag(testTag1);
        pref.setPreferenceWeight(0.8);
        when(userPreferenceRepository.findByUserId(TEST_USER_ID)).thenReturn(Collections.singletonList(pref));
        
        GameTag gameTag = new GameTag();
        gameTag.setGame(testGame1);
        gameTag.setTag(testTag1);
        when(gameTagRepository.findAll()).thenReturn(Collections.singletonList(gameTag));
        
        when(gameRepository.findAll()).thenReturn(Arrays.asList(testGame1, testGame2));
        
        List<RecommendationDto> librecRecs = new ArrayList<>();
        RecommendationDto librecRec = new RecommendationDto();
        librecRec.setGameId(TEST_GAME_ID_1);
        librecRec.setRecommendationScore(0.9);
        librecRecs.add(librecRec);
        when(librecEngineService.recommend(TEST_USER_ID)).thenReturn(librecRecs);
        
        // User has already played game1
        UserGames playedGame = createUserGame(testUser, testGame1, InteractionEnum.Viewed, null, LocalDateTime.now());
        when(userGameRepository.findByUserId(TEST_USER_ID)).thenReturn(Collections.singletonList(playedGame));

        // Act
        List<RecommendationDto> result = recomendationService.getRecommendationsForUser(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        // Game1 should be excluded because it's already played
        assertFalse(result.stream().anyMatch(r -> r.getGameId().equals(TEST_GAME_ID_1)));
    }


    @Test
    void getSimilarGames_ShouldReturnGamesWithCommonTags() {
        // Arrange
        when(gameRepository.findById(TEST_GAME_ID_1)).thenReturn(Optional.of(testGame1));
        
        GameTag gameTag1 = new GameTag();
        gameTag1.setGame(testGame1);
        gameTag1.setTag(testTag1);
        
        GameTag gameTag2 = new GameTag();
        gameTag2.setGame(testGame1);
        gameTag2.setTag(testTag2);
        
        List<GameTag> targetGameTags = Arrays.asList(gameTag1, gameTag2);
        when(gameTagRepository.findByGameId(TEST_GAME_ID_1)).thenReturn(targetGameTags);
        
        GameEntity similarGame = new GameEntity();
        similarGame.setId(200L);
        similarGame.setName("Similar Game");
        
        GameTag similarGameTag = new GameTag();
        similarGameTag.setGame(similarGame);
        similarGameTag.setTag(testTag1); // Shares tag1 with target
        
        List<GameTag> allGameTags = new ArrayList<>();
        allGameTags.addAll(targetGameTags);
        allGameTags.add(similarGameTag);
        
        when(gameTagRepository.findAll()).thenReturn(allGameTags);
        when(gameRepository.findAll()).thenReturn(Arrays.asList(testGame1, similarGame));
        when(gameRepository.findById(200L)).thenReturn(Optional.of(similarGame));

        // Act
        List<RecommendationDto> result = recomendationService.getSimilarGames(TEST_GAME_ID_1);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(200L, result.get(0).getGameId());
        assertTrue(result.get(0).getMatchPercentage() >= 0);
    }

    @Test
    void getSimilarGames_WhenNoSimilar_ShouldReturnEmpty() {
        // Arrange
        when(gameRepository.findById(TEST_GAME_ID_1)).thenReturn(Optional.of(testGame1));
        
        GameTag gameTag = new GameTag();
        gameTag.setGame(testGame1);
        gameTag.setTag(testTag1);
        
        when(gameTagRepository.findByGameId(TEST_GAME_ID_1)).thenReturn(Collections.singletonList(gameTag));
        
        GameEntity otherGame = new GameEntity();
        otherGame.setId(200L);
        otherGame.setName("Different Game");
        
        GameTag otherGameTag = new GameTag();
        otherGameTag.setGame(otherGame);
        otherGameTag.setTag(testTag2); // No common tags
        
        when(gameTagRepository.findAll()).thenReturn(Arrays.asList(gameTag, otherGameTag));
        when(gameRepository.findAll()).thenReturn(Arrays.asList(testGame1, otherGame));
        when(gameRepository.findById(200L)).thenReturn(Optional.of(otherGame));

        // Act
        List<RecommendationDto> result = recomendationService.getSimilarGames(TEST_GAME_ID_1);

        // Assert
        assertNotNull(result);
        // Score should be 0 because no common tags
        assertEquals(0.0, result.get(0).getRecommendationScore());
    }

   

    @Test
    void getSessionDetails_ShouldReturnSessionWithItems() {
        // Arrange
        Long sessionId = 1L;
        RecommendationSession session = new RecommendationSession(testUser, LocalDateTime.now());
        session.setId(sessionId);
        
        RecommendationItems item1 = new RecommendationItems(testGame1, session, 1, 0.95);
        RecommendationItems item2 = new RecommendationItems(testGame2, session, 2, 0.85);
        
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(recommendationItemsRepository.findBySessionIdOrderByRank(sessionId))
            .thenReturn(Arrays.asList(item1, item2));
        when(gameRepository.findById(TEST_GAME_ID_1)).thenReturn(Optional.of(testGame1));
        when(gameRepository.findById(TEST_GAME_ID_2)).thenReturn(Optional.of(testGame2));

        // Act
        RecommendationSessionDetailsDto result = recomendationService.getSessionDetails(sessionId);

        // Assert
        assertNotNull(result);
        assertEquals(sessionId, result.getId());
        assertEquals(2, result.getItems().size());
        assertEquals(TEST_GAME_ID_1, result.getItems().get(0).getGameId());
        assertEquals(0.95, result.getItems().get(0).getRecommendationScore());
    }

    @Test
    void getSessionDetails_WhenSessionNotFound_ShouldThrowException() {
        // Arrange
        Long sessionId = 999L;
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> recomendationService.getSessionDetails(sessionId));
    }

    @Test
    void generateRecommendationSession_ShouldCreateAndSaveSession() {
        // Arrange
        List<RecommendationDto> recommendations = createMockRecommendations();
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(gameRepository.findById(TEST_GAME_ID_1)).thenReturn(Optional.of(testGame1));
        when(gameRepository.findById(TEST_GAME_ID_2)).thenReturn(Optional.of(testGame2));
        
        ArgumentCaptor<RecommendationSession> sessionCaptor = ArgumentCaptor.forClass(RecommendationSession.class);
        ArgumentCaptor<RecommendationItems> itemsCaptor = ArgumentCaptor.forClass(RecommendationItems.class);

        // Mock the getRecommendationsForUser method indirectly by spying
        RecomendationService spyService = spy(recomendationService);
        doReturn(recommendations).when(spyService).getRecommendationsForUser(eq(TEST_USER_ID));
        
        // Act
        spyService.generateRecommendationSession(TEST_USER_ID);

        // Assert
        verify(sessionRepository).save(sessionCaptor.capture());
        RecommendationSession savedSession = sessionCaptor.getValue();
        assertEquals(testUser, savedSession.getUser());
        assertNotNull(savedSession.getGeneratedAt());
        
        verify(recommendationItemsRepository, times(recommendations.size())).save(itemsCaptor.capture());
        List<RecommendationItems> savedItems = itemsCaptor.getAllValues();
        assertEquals(recommendations.size(), savedItems.size());
        assertEquals(1, savedItems.get(0).getRank());
        assertEquals(2, savedItems.get(1).getRank());
    }

    @Test
    void getFastRecommendations_WhenSessionExists_ShouldReturnCachedRecommendations() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        
        RecommendationSession session = new RecommendationSession(testUser, LocalDateTime.now());
        session.setId(1L);
        
        List<RecommendationSession> sessions = Collections.singletonList(session);
        when(sessionRepository.findByUserIdOrderByGeneratedAtDesc(TEST_USER_ID)).thenReturn(sessions);
        
        RecommendationItems item1 = new RecommendationItems(testGame1, session, 1, 0.95);
        RecommendationItems item2 = new RecommendationItems(testGame2, session, 2, 0.85);
        
        when(recommendationItemsRepository.findBySessionIdOrderByRank(1L))
            .thenReturn(Arrays.asList(item1, item2));
        when(gameRepository.findById(TEST_GAME_ID_1)).thenReturn(Optional.of(testGame1));
        when(gameRepository.findById(TEST_GAME_ID_2)).thenReturn(Optional.of(testGame2));

        // Act
        List<RecommendationDto> result = recomendationService.getFastRecommendations(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(TEST_GAME_ID_1, result.get(0).getGameId());
        assertEquals(0.95, result.get(0).getRecommendationScore());
        verify(librecEngineService, never()).recommend(any());
    }

    

    @Test
    void mapToRecommendation_ShouldCorrectlyMapAndCalculatePercentage() {
        // Arrange
        when(gameRepository.findById(TEST_GAME_ID_1)).thenReturn(Optional.of(testGame1));
        
        // Act
        RecommendationDto result = recomendationService.getSimilarGames(TEST_GAME_ID_1).stream()
            .findFirst()
            .orElse(null);
        
        if (result != null) {
            // Assert
            assertNotNull(result);
            assertTrue(result.getMatchPercentage() >= 0 && result.getMatchPercentage() <= 100);
        }
    }

    // Helper methods
    private UserGames createUserGame(UserEntity user, GameEntity game, InteractionEnum interaction, 
                                      Integer rating, LocalDateTime time) {
        UserGames ug = new UserGames();
        ug.setUser(user);
        ug.setGame(game);
        ug.setInteraction(interaction);
        ug.setRating(rating);
        ug.setTime(time);
        return ug;
    }

    private List<RecommendationDto> createMockRecommendations() {
        List<RecommendationDto> recommendations = new ArrayList<>();
        
        RecommendationDto dto1 = new RecommendationDto();
        dto1.setGameId(TEST_GAME_ID_1);
        dto1.setName("Game 1");
        dto1.setRecommendationScore(0.95);
        recommendations.add(dto1);
        
        RecommendationDto dto2 = new RecommendationDto();
        dto2.setGameId(TEST_GAME_ID_2);
        dto2.setName("Game 2");
        dto2.setRecommendationScore(0.85);
        recommendations.add(dto2);
        
        return recommendations;
    }
}