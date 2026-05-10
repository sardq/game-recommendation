package com.diplome.game_recommendation.services;

import com.diplome.game_recommendation.dtos.rawg.*;
import com.diplome.game_recommendation.integration.RawgApiService;
import com.diplome.game_recommendation.integration.VideoApiService;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.InteractionEnum;
import com.diplome.game_recommendation.models.PlatformEnum;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.repositories.GameRepository;
import com.diplome.game_recommendation.repositories.UserGameRepository;
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
import org.springframework.data.domain.Pageable;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private RawgApiService rawgApiService;

    @Mock
    private VideoApiService videoApiService;

    @Mock
    private UserGameRepository userGameRepository;

    @InjectMocks
    private GameService gameService;

    private GameEntity testGame;
    private final Long TEST_GAME_ID = 253L;
    private final Long TEST_USER_ID = 100L;
    private final Long TEST_TAG_ID = 10L;

    @BeforeEach
    void setUp() {
        testGame = new GameEntity();
        testGame.setId(TEST_GAME_ID);
        testGame.setName("Test Game");
        testGame.setRawgId(12345L);
        testGame.setDescription(null);
        testGame.setRating(4.5);
        testGame.setLocalRating(0.0);
        testGame.setLocalRatingCount(0);
        testGame.setReleaseDate(Date.valueOf(LocalDate.of(2023, 1, 1)));
        testGame.setPosterUrl("http://test.com/poster.jpg");
        testGame.setPlaytime(20);
        testGame.setPlatforms(new HashSet<>());
        testGame.setScreenshotUrls(new ArrayList<>());
        testGame.setTrailerUrls(new ArrayList<>());
        testGame.setWalkthroughUrls(new ArrayList<>());
        testGame.setStoreLinks(new ArrayList<>());
    }

    @Test
    void getGames_ShouldReturnPageOfGames() {
         
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<GameEntity> games = Arrays.asList(testGame, new GameEntity());
        Page<GameEntity> expectedPage = new PageImpl<>(games, pageable, games.size());

        when(gameRepository.findAll(pageable)).thenReturn(expectedPage);

        
        Page<GameEntity> result = gameService.getGames(page, size);

         
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(expectedPage, result);
        verify(gameRepository).findAll(pageable);
    }

    @Test
    void getGamesByTag_ShouldReturnPageOfGames() {
         
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<GameEntity> games = Collections.singletonList(testGame);
        Page<GameEntity> expectedPage = new PageImpl<>(games, pageable, games.size());

        when(gameRepository.findByTagId(eq(TEST_TAG_ID), eq(pageable))).thenReturn(expectedPage);

        
        Page<GameEntity> result = gameService.getGamesByTag(TEST_TAG_ID, page, size);

         
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(gameRepository).findByTagId(TEST_TAG_ID, pageable);
    }

    @Test
    void getAllByFilters_WithoutSearch_ShouldReturnAllGames() {
         
        String search = null;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<GameEntity> expectedPage = new PageImpl<>(Collections.singletonList(testGame), pageable, 1);

        when(gameRepository.findAll(pageable)).thenReturn(expectedPage);

        
        Page<GameEntity> result = gameService.getAllByFilters(search, page, size);

         
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(gameRepository).findAll(pageable);
        verify(gameRepository, never()).filterBySearch(any(), any());
    }

    @Test
    void getAllByFilters_WithEmptySearch_ShouldReturnAllGames() {
         
        String search = "";
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<GameEntity> expectedPage = new PageImpl<>(Collections.singletonList(testGame), pageable, 1);

        when(gameRepository.findAll(pageable)).thenReturn(expectedPage);

        
        Page<GameEntity> result = gameService.getAllByFilters(search, page, size);

         
        assertNotNull(result);
        verify(gameRepository).findAll(pageable);
    }

    @Test
    void getGame_WhenExists_ShouldReturnGame() {
         
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));

        
        GameEntity result = gameService.getGame(TEST_GAME_ID);

         
        assertNotNull(result);
        assertEquals(TEST_GAME_ID, result.getId());
        assertEquals("Test Game", result.getName());
        verify(gameRepository).findById(TEST_GAME_ID);
    }

    @Test
    void getGame_WhenNotExists_ShouldReturnNull() {
         
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.empty());

        
        GameEntity result = gameService.getGame(TEST_GAME_ID);

         
        assertNull(result);
        verify(gameRepository).findById(TEST_GAME_ID);
    }

    @Test
    void getFavorites_ShouldReturnListOfFavoriteGames() {
         
        UserGames userGame1 = new UserGames();
        userGame1.setGame(testGame);
        
        GameEntity game2 = new GameEntity();
        game2.setId(2L);
        UserGames userGame2 = new UserGames();
        userGame2.setGame(game2);

        List<UserGames> userGamesList = Arrays.asList(userGame1, userGame2);

        when(userGameRepository.findByUserIdAndInteraction(TEST_USER_ID, InteractionEnum.Favorite))
            .thenReturn(userGamesList);

        
        List<GameEntity> result = gameService.getFavorites(TEST_USER_ID);

         
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(TEST_GAME_ID, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        verify(userGameRepository).findByUserIdAndInteraction(TEST_USER_ID, InteractionEnum.Favorite);
    }

    @Test
    void getFavorites_WhenNoFavorites_ShouldReturnEmptyList() {
         
        when(userGameRepository.findByUserIdAndInteraction(TEST_USER_ID, InteractionEnum.Favorite))
            .thenReturn(new ArrayList<>());

        
        List<GameEntity> result = gameService.getFavorites(TEST_USER_ID);

         
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userGameRepository).findByUserIdAndInteraction(TEST_USER_ID, InteractionEnum.Favorite);
    }

    @Test
    void getPopularGames_ShouldReturnOrderedGames() {
         
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<GameEntity> games = Arrays.asList(testGame, new GameEntity());
        Page<GameEntity> expectedPage = new PageImpl<>(games, pageable, games.size());

        when(gameRepository.findByOrderByReleaseDateDesc(pageable)).thenReturn(expectedPage);

        
        Page<GameEntity> result = gameService.getPopularGames(page, size);

         
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(gameRepository).findByOrderByReleaseDateDesc(pageable);
    }

    @Test
    void loadGameIfNeeded_WhenDescriptionExists_ShouldReturnExisting() {
         
        testGame.setDescription("Existing description");
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));

        
        GameEntity result = gameService.loadGameIfNeeded(TEST_GAME_ID);

         
        assertNotNull(result);
        assertEquals("Existing description", result.getDescription());
        verify(gameRepository).findById(TEST_GAME_ID);
        verify(rawgApiService, never()).getGameDetails(any());
        verify(gameRepository, never()).save(any());
    }

    @Test
    void loadGameIfNeeded_WhenDescriptionMissing_ShouldLoadFromExternal() {
         
        RawgGameDetailsResponse detailsResponse = createMockGameDetailsResponse();
        List<RawgStoreResponse.StoreResult> stores = createMockStores();
        List<String> screenshots = Arrays.asList("shot1.jpg", "shot2.jpg");
        List<String> trailers = Arrays.asList("trailer1.mp4", "trailer2.mp4");
        List<String> walkthroughs = Arrays.asList("walk1.mp4", "walk2.mp4");

        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        when(rawgApiService.getGameDetails(testGame.getRawgId())).thenReturn(detailsResponse);
        when(rawgApiService.getGameStores(testGame.getRawgId())).thenReturn(stores);
        when(rawgApiService.getGameScreenshots(testGame.getRawgId())).thenReturn(screenshots);
        when(videoApiService.searchVideos(eq(testGame.getName()), eq("gameplay trailer"), eq(3)))
            .thenReturn(trailers);
        when(videoApiService.searchVideos(eq(testGame.getName()), eq("Прохождение"), eq(3)))
            .thenReturn(walkthroughs);
        when(gameRepository.save(any(GameEntity.class))).thenReturn(testGame);

        
        GameEntity result = gameService.loadGameIfNeeded(TEST_GAME_ID);

         
        assertNotNull(result);
        assertEquals("Test description", result.getDescription());
        assertEquals(4.7, result.getRating());
        assertEquals(85.0, result.getMetacriticRate());
        assertEquals(25, result.getPlaytime());
        assertEquals(screenshots, result.getScreenshotUrls());
        assertEquals(trailers, result.getTrailerUrls());
        assertEquals(walkthroughs, result.getWalkthroughUrls());
        
        verify(gameRepository).findById(TEST_GAME_ID);
        verify(rawgApiService).getGameDetails(testGame.getRawgId());
        verify(rawgApiService).getGameStores(testGame.getRawgId());
        verify(rawgApiService).getGameScreenshots(testGame.getRawgId());
        verify(videoApiService, times(2)).searchVideos(any(), any(), anyInt());
        verify(gameRepository).save(any(GameEntity.class));
    }

    @Test
    void loadGameIfNeeded_WhenGameNotFound_ShouldThrowException() {
         
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> gameService.loadGameIfNeeded(TEST_GAME_ID));
        
        assertEquals("Игра не найдена", exception.getMessage());
        verify(gameRepository).findById(TEST_GAME_ID);
        verify(rawgApiService, never()).getGameDetails(any());
    }

    

    @Test
    void updateLocalRating_ShouldCalculateAndUpdateRating() {
         
        Double averageRating = 4.2;
        Integer ratingCount = 15;
        
        when(userGameRepository.getAverageRatingForGame(TEST_GAME_ID)).thenReturn(averageRating);
        when(userGameRepository.getCountOfRatingsForGame(TEST_GAME_ID)).thenReturn(ratingCount);
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        when(gameRepository.save(any(GameEntity.class))).thenReturn(testGame);

        
        gameService.updateLocalRating(TEST_GAME_ID);

         
        assertEquals(4.2, testGame.getLocalRating());
        assertEquals(15, testGame.getLocalRatingCount());
        
        ArgumentCaptor<GameEntity> captor = ArgumentCaptor.forClass(GameEntity.class);
        verify(gameRepository).save(captor.capture());
        
        GameEntity savedGame = captor.getValue();
        assertEquals(4.2, savedGame.getLocalRating());
        assertEquals(15, savedGame.getLocalRatingCount());
        
        verify(userGameRepository).getAverageRatingForGame(TEST_GAME_ID);
        verify(userGameRepository).getCountOfRatingsForGame(TEST_GAME_ID);
        verify(gameRepository).findById(TEST_GAME_ID);
    }

    @Test
    void updateLocalRating_WhenNoRatings_ShouldSetZero() {
         
        when(userGameRepository.getAverageRatingForGame(TEST_GAME_ID)).thenReturn(null);
        when(userGameRepository.getCountOfRatingsForGame(TEST_GAME_ID)).thenReturn(null);
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.of(testGame));
        when(gameRepository.save(any(GameEntity.class))).thenReturn(testGame);

        
        gameService.updateLocalRating(TEST_GAME_ID);

         
        assertEquals(0.0, testGame.getLocalRating());
        assertEquals(0, testGame.getLocalRatingCount());
    }

    @Test
    void updateLocalRating_WhenGameNotFound_ShouldThrowException() {
         
        when(userGameRepository.getAverageRatingForGame(TEST_GAME_ID)).thenReturn(4.0);
        when(userGameRepository.getCountOfRatingsForGame(TEST_GAME_ID)).thenReturn(10);
        when(gameRepository.findById(TEST_GAME_ID)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> gameService.updateLocalRating(TEST_GAME_ID));
        
        assertEquals("Игра не найдена", exception.getMessage());
        verify(gameRepository, never()).save(any());
    }


    private RawgGameDetailsResponse createMockGameDetailsResponse() {
        RawgGameDetailsResponse response = new RawgGameDetailsResponse();
        response.setDescription("Test description");
        response.setRating(4.7);
        response.setReleased("2023-05-15");
        response.setMetacritic(85);
        response.setBackground_image("http://test.com/background.jpg");
        response.setPlaytime(25);
        response.setPlatforms(new ArrayList<>());
        return response;
    }

    private List<RawgStoreResponse.StoreResult> createMockStores() {
        RawgStoreResponse.StoreResult store = new RawgStoreResponse.StoreResult();
        store.setUrl("http://store.test.com/game");
        return Collections.singletonList(store);
    }

}