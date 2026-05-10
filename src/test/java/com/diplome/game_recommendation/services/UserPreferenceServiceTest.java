package com.diplome.game_recommendation.services;

import com.diplome.game_recommendation.dtos.TagPreferenceDto;
import com.diplome.game_recommendation.models.*;
import com.diplome.game_recommendation.repositories.GameTagRepository;
import com.diplome.game_recommendation.repositories.TagRepository;
import com.diplome.game_recommendation.repositories.UserGameRepository;
import com.diplome.game_recommendation.repositories.UserPreferenceRepository;
import com.diplome.game_recommendation.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private UserGameRepository userGamesRepository;

    @Mock
    private GameTagRepository gameTagRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserPreferenceService userPreferenceService;

    private UserEntity testUser;
    private GameEntity testGame1;
    private GameEntity testGame2;
    private TagEntity testTag1;
    private TagEntity testTag2;
    private TagEntity testTag3;
    
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_GAME_ID_1 = 100L;
    private final Long TEST_GAME_ID_2 = 101L;
    private final Long TEST_TAG_ID_1 = 10L;
    private final Long TEST_TAG_ID_2 = 11L;
    private final Long TEST_TAG_ID_3 = 12L;
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

        testGame2 = new GameEntity();
        testGame2.setId(TEST_GAME_ID_2);
        testGame2.setName("Game 2");

        testTag1 = new TagEntity();
        testTag1.setId(TEST_TAG_ID_1);
        testTag1.setName("Action");

        testTag2 = new TagEntity();
        testTag2.setId(TEST_TAG_ID_2);
        testTag2.setName("RPG");

        testTag3 = new TagEntity();
        testTag3.setId(TEST_TAG_ID_3);
        testTag3.setName("Strategy");

        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
    }

    @Test
    void getUserPreferences_ShouldReturnUserPreferences() {
        // Arrange
        UserPreference pref1 = new UserPreference();
        pref1.setUser(testUser);
        pref1.setTag(testTag1);
        pref1.setPreferenceWeight(0.8);
        
        UserPreference pref2 = new UserPreference();
        pref2.setUser(testUser);
        pref2.setTag(testTag2);
        pref2.setPreferenceWeight(0.6);
        
        List<UserPreference> expectedPrefs = Arrays.asList(pref1, pref2);
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userPreferenceRepository.findByUserId(TEST_USER_ID)).thenReturn(expectedPrefs);

        // Act
        List<UserPreference> result = userPreferenceService.getUserPreferences(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testTag1, result.get(0).getTag());
        assertEquals(0.8, result.get(0).getPreferenceWeight());
        assertEquals(testTag2, result.get(1).getTag());
        assertEquals(0.6, result.get(1).getPreferenceWeight());
        
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(userPreferenceRepository).findByUserId(TEST_USER_ID);
    }


    @Test
    void getUserPreferences_WhenNoPreferences_ShouldReturnEmptyList() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userPreferenceRepository.findByUserId(TEST_USER_ID)).thenReturn(new ArrayList<>());

        // Act
        List<UserPreference> result = userPreferenceService.getUserPreferences(authentication);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void updateUserPreferences_ShouldCalculateAndSavePreferences() {
        // Arrange
        // Create interactions
        UserGames viewedGame = createUserGame(testUser, testGame1, InteractionEnum.Viewed, null, LocalDateTime.now());
        UserGames favoriteGame = createUserGame(testUser, testGame2, InteractionEnum.Favorite, null, LocalDateTime.now());
        UserGames ratedGame = createUserGame(testUser, testGame1, InteractionEnum.Rated, 8, LocalDateTime.now());
        
        List<UserGames> interactions = Arrays.asList(viewedGame, favoriteGame, ratedGame);
        
        // Create game tags
        GameTag gameTag1 = new GameTag();
        gameTag1.setGame(testGame1);
        gameTag1.setTag(testTag1);
        
        GameTag gameTag2 = new GameTag();
        gameTag2.setGame(testGame2);
        gameTag2.setTag(testTag2);
        
        GameTag gameTag3 = new GameTag();
        gameTag3.setGame(testGame1);
        gameTag3.setTag(testTag3);
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findByUserId(TEST_USER_ID)).thenReturn(interactions);
        
        when(gameTagRepository.findByGameId(TEST_GAME_ID_1)).thenReturn(Arrays.asList(gameTag1, gameTag3));
        when(gameTagRepository.findByGameId(TEST_GAME_ID_2)).thenReturn(Collections.singletonList(gameTag2));
        
        when(tagRepository.findById(TEST_TAG_ID_1)).thenReturn(Optional.of(testTag1));
        when(tagRepository.findById(TEST_TAG_ID_2)).thenReturn(Optional.of(testTag2));
        when(tagRepository.findById(TEST_TAG_ID_3)).thenReturn(Optional.of(testTag3));
        
        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);

        // Act
        userPreferenceService.updateUserPreferences(TEST_USER_ID);

        // Assert
        verify(userPreferenceRepository).deleteByUserId(TEST_USER_ID);
        verify(userPreferenceRepository, atLeastOnce()).save(captor.capture());
        
        List<UserPreference> savedPrefs = captor.getAllValues();
        
        // Verify weights are calculated correctly
        // Viewed: 0.2, Rated: 0.8, Favorite: 0.8
        // For tag1: from viewed (0.2) + rated (0.8) = 1.0
        // For tag3: from rated (0.8) = 0.8
        // For tag2: from favorite (0.8) = 0.8
        
        Map<Long, Double> savedWeights = new HashMap<>();
        for (UserPreference pref : savedPrefs) {
            savedWeights.put(pref.getTag().getId(), pref.getPreferenceWeight());
        }
        
        assertEquals(1.0, savedWeights.get(TEST_TAG_ID_1));
        assertEquals(0.8, savedWeights.get(TEST_TAG_ID_2));
        assertEquals(0.8, savedWeights.get(TEST_TAG_ID_3));
    }

    @Test
    void updateUserPreferences_WithNoInteractions_ShouldDeleteOnly() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findByUserId(TEST_USER_ID)).thenReturn(new ArrayList<>());

        // Act
        userPreferenceService.updateUserPreferences(TEST_USER_ID);

        // Assert
        verify(userPreferenceRepository).deleteByUserId(TEST_USER_ID);
        verify(userPreferenceRepository, never()).save(any());
        verify(gameTagRepository, never()).findByGameId(any());
    }


    @Test
    void updateUserPreferences_WithFavoriteInteractions() {
        // Arrange
        UserGames favoriteGame = createUserGame(testUser, testGame1, InteractionEnum.Favorite, null, LocalDateTime.now());
        
        List<UserGames> interactions = Collections.singletonList(favoriteGame);
        
        GameTag gameTag = new GameTag();
        gameTag.setGame(testGame1);
        gameTag.setTag(testTag1);
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findByUserId(TEST_USER_ID)).thenReturn(interactions);
        when(gameTagRepository.findByGameId(TEST_GAME_ID_1)).thenReturn(Collections.singletonList(gameTag));
        when(tagRepository.findById(TEST_TAG_ID_1)).thenReturn(Optional.of(testTag1));
        
        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);

        // Act
        userPreferenceService.updateUserPreferences(TEST_USER_ID);

        // Assert
        verify(userPreferenceRepository).save(captor.capture());
        UserPreference savedPref = captor.getValue();
        
        // Favorite weight is 0.8
        assertEquals(0.8, savedPref.getPreferenceWeight());
    }

    @Test
    void updateUserPreferences_WithRatedInteractions() {
        // Arrange
        UserGames ratedGame = createUserGame(testUser, testGame1, InteractionEnum.Rated, 7, LocalDateTime.now());
        
        List<UserGames> interactions = Collections.singletonList(ratedGame);
        
        GameTag gameTag = new GameTag();
        gameTag.setGame(testGame1);
        gameTag.setTag(testTag1);
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findByUserId(TEST_USER_ID)).thenReturn(interactions);
        when(gameTagRepository.findByGameId(TEST_GAME_ID_1)).thenReturn(Collections.singletonList(gameTag));
        when(tagRepository.findById(TEST_TAG_ID_1)).thenReturn(Optional.of(testTag1));
        
        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);

        // Act
        userPreferenceService.updateUserPreferences(TEST_USER_ID);

        // Assert
        verify(userPreferenceRepository).save(captor.capture());
        UserPreference savedPref = captor.getValue();
        
        // Rated weight = rating / 10.0 = 0.7
        assertEquals(0.7, savedPref.getPreferenceWeight());
    }

    @Test
    void updateUserPreferences_WithMultipleTagsPerGame() {
        // Arrange
        UserGames viewedGame = createUserGame(testUser, testGame1, InteractionEnum.Viewed, null, LocalDateTime.now());
        
        List<UserGames> interactions = Collections.singletonList(viewedGame);
        
        GameTag gameTag1 = new GameTag();
        gameTag1.setGame(testGame1);
        gameTag1.setTag(testTag1);
        
        GameTag gameTag2 = new GameTag();
        gameTag2.setGame(testGame1);
        gameTag2.setTag(testTag2);
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findByUserId(TEST_USER_ID)).thenReturn(interactions);
        when(gameTagRepository.findByGameId(TEST_GAME_ID_1)).thenReturn(Arrays.asList(gameTag1, gameTag2));
        when(tagRepository.findById(TEST_TAG_ID_1)).thenReturn(Optional.of(testTag1));
        when(tagRepository.findById(TEST_TAG_ID_2)).thenReturn(Optional.of(testTag2));
        
        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);

        // Act
        userPreferenceService.updateUserPreferences(TEST_USER_ID);

        // Assert
        verify(userPreferenceRepository, times(2)).save(captor.capture());
        List<UserPreference> savedPrefs = captor.getAllValues();
        
        assertEquals(2, savedPrefs.size());
        // Both tags should have the same weight from the viewed interaction
        assertEquals(0.2, savedPrefs.get(0).getPreferenceWeight());
        assertEquals(0.2, savedPrefs.get(1).getPreferenceWeight());
    }

    @Test
    void initializeColdStartPreferences_ShouldSavePreferencesWithWeight1() {
        // Arrange
        List<Long> tagIds = Arrays.asList(TEST_TAG_ID_1, TEST_TAG_ID_2, TEST_TAG_ID_3);
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(tagRepository.findById(TEST_TAG_ID_1)).thenReturn(Optional.of(testTag1));
        when(tagRepository.findById(TEST_TAG_ID_2)).thenReturn(Optional.of(testTag2));
        when(tagRepository.findById(TEST_TAG_ID_3)).thenReturn(Optional.of(testTag3));
        
        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);

        // Act
        userPreferenceService.initializeColdStartPreferences(TEST_USER_ID, tagIds);

        // Assert
        verify(userPreferenceRepository, times(3)).save(captor.capture());
        List<UserPreference> savedPrefs = captor.getAllValues();
        
        assertEquals(3, savedPrefs.size());
        for (UserPreference pref : savedPrefs) {
            assertEquals(testUser, pref.getUser());
            assertEquals(1.0, pref.getPreferenceWeight());
        }
        
        Set<Long> savedTagIds = new HashSet<>();
        for (UserPreference pref : savedPrefs) {
            savedTagIds.add(pref.getTag().getId());
        }
        
        assertTrue(savedTagIds.containsAll(tagIds));
    }

    @Test
    void initializeColdStartPreferences_WhenTagNotFound_ShouldThrowException() {
        // Arrange
        List<Long> tagIds = Collections.singletonList(TEST_TAG_ID_1);
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(tagRepository.findById(TEST_TAG_ID_1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, 
            () -> userPreferenceService.initializeColdStartPreferences(TEST_USER_ID, tagIds));
        
        verify(userPreferenceRepository, never()).save(any());
    }

    @Test
    void initializeColdStartPreferencesWithRating_ShouldSavePreferencesWithCalculatedWeights() {
        // Arrange
        List<TagPreferenceDto> tagPreferences = Arrays.asList(
            createTagPreferenceDto(TEST_TAG_ID_1, 5.0), // weight = 1.0
            createTagPreferenceDto(TEST_TAG_ID_2, 3.0), // weight = 0.6
            createTagPreferenceDto(TEST_TAG_ID_3, 1.0)  // weight = 0.2
        );
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tagRepository.findById(TEST_TAG_ID_1)).thenReturn(Optional.of(testTag1));
        when(tagRepository.findById(TEST_TAG_ID_2)).thenReturn(Optional.of(testTag2));
        when(tagRepository.findById(TEST_TAG_ID_3)).thenReturn(Optional.of(testTag3));
        
        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);

        // Act
        userPreferenceService.initializeColdStartPreferencesWithRating(authentication, tagPreferences);

        // Assert
        verify(userPreferenceRepository).deleteByUserId(TEST_USER_ID);
        verify(userPreferenceRepository, times(3)).save(captor.capture());
        
        List<UserPreference> savedPrefs = captor.getAllValues();
        Map<Long, Double> weightMap = new HashMap<>();
        for (UserPreference pref : savedPrefs) {
            weightMap.put(pref.getTag().getId(), pref.getPreferenceWeight());
        }
        
        assertEquals(1.0, weightMap.get(TEST_TAG_ID_1));
        assertEquals(0.6, weightMap.get(TEST_TAG_ID_2));
        assertEquals(0.2, weightMap.get(TEST_TAG_ID_3));
    }

    @Test
    void initializeColdStartPreferencesWithRating_ShouldDeleteExistingPreferences() {
        // Arrange
        List<TagPreferenceDto> tagPreferences = Collections.singletonList(
            createTagPreferenceDto(TEST_TAG_ID_1, 4.0)
        );
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tagRepository.findById(TEST_TAG_ID_1)).thenReturn(Optional.of(testTag1));

        // Act
        userPreferenceService.initializeColdStartPreferencesWithRating(authentication, tagPreferences);

        // Assert
        verify(userPreferenceRepository).deleteByUserId(TEST_USER_ID);
        verify(userPreferenceRepository).save(any(UserPreference.class));
    }

    @Test
    void countByUserId_ShouldReturnPreferenceCount() {
        // Arrange
        Long expectedCount = 5L;
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userPreferenceRepository.countByUserId(TEST_USER_ID)).thenReturn(expectedCount);

        // Act
        Long result = userPreferenceService.countByUserId(authentication);

        // Assert
        assertEquals(expectedCount, result);
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(userPreferenceRepository).countByUserId(TEST_USER_ID);
    }

    @Test
    void countByUserId_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, 
            () -> userPreferenceService.countByUserId(authentication));
        
        verify(userPreferenceRepository, never()).countByUserId(any());
    }

    @Test
    void countByUserId_WhenNoPreferences_ShouldReturnZero() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userPreferenceRepository.countByUserId(TEST_USER_ID)).thenReturn(0L);

        // Act
        Long result = userPreferenceService.countByUserId(authentication);

        // Assert
        assertEquals(0L, result);
    }

    @Test
    void updateUserPreferences_ShouldAccumulateWeightsFromMultipleInteractions() {
        // Arrange
        // Multiple interactions for the same game and tag
        UserGames viewedGame = createUserGame(testUser, testGame1, InteractionEnum.Viewed, null, LocalDateTime.now());
        UserGames favoriteGame = createUserGame(testUser, testGame1, InteractionEnum.Favorite, null, LocalDateTime.now());
        UserGames ratedGame = createUserGame(testUser, testGame1, InteractionEnum.Rated, 9, LocalDateTime.now());
        
        List<UserGames> interactions = Arrays.asList(viewedGame, favoriteGame, ratedGame);
        
        GameTag gameTag = new GameTag();
        gameTag.setGame(testGame1);
        gameTag.setTag(testTag1);
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userGamesRepository.findByUserId(TEST_USER_ID)).thenReturn(interactions);
        when(gameTagRepository.findByGameId(TEST_GAME_ID_1)).thenReturn(Collections.singletonList(gameTag));
        when(tagRepository.findById(TEST_TAG_ID_1)).thenReturn(Optional.of(testTag1));
        
        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);

        // Act
        userPreferenceService.updateUserPreferences(TEST_USER_ID);

        // Assert
        verify(userPreferenceRepository).save(captor.capture());
        UserPreference savedPref = captor.getValue();
        
        // Total weight: viewed(0.2) + favorite(0.8) + rated(0.9) = 1.9
        assertEquals(1.9, savedPref.getPreferenceWeight());
    }

    @Test
    void updateUserPreferences_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, 
            () -> userPreferenceService.updateUserPreferences(TEST_USER_ID));
        
        verify(userGamesRepository, never()).findByUserId(any());
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

    private TagPreferenceDto createTagPreferenceDto(Long tagId, Double rating) {
        TagPreferenceDto dto = new TagPreferenceDto();
        dto.tagId = tagId;
        dto.rating = rating;
        return dto;
    }
}
