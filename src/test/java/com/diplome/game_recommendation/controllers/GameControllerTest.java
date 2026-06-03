package com.diplome.game_recommendation.controllers;

import com.diplome.game_recommendation.dtos.*;
import com.diplome.game_recommendation.integration.CurrencyService;
import com.diplome.game_recommendation.integration.NewsService;
import com.diplome.game_recommendation.integration.PriceService;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.GameTag;
import com.diplome.game_recommendation.models.PlatformEnum;
import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.services.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private PriceService priceService;

    @Mock
    private NewsService newsService;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private ModelMapper mapper;

    @InjectMocks
    private GameController gameController;

    private GameEntity testGame1;
    private GameEntity testGame2;
    private GameDto testGameDto1;
    private GameDto testGameDto2;
    private GameDetailsDto testGameDetailsDto;
    private TagEntity testTag;
    private GameTag testGameTag;
    private List<PriceService.GameDeal> testDeals;
    private Double testUsdRate;

    private final Long TEST_GAME_ID_1 = 1L;
    private final Long TEST_GAME_ID_2 = 2L;
    private final Long TEST_TAG_ID = 10L;
    private final Long TEST_USER_ID = 100L;
    private final String TEST_GAME_NAME = "Test Game";
    private final String TEST_SEARCH_QUERY = "action";

    @BeforeEach
    void setUp() {
        testGame1 = new GameEntity();
        testGame1.setId(TEST_GAME_ID_1);
        testGame1.setName("Test Game 1");
        testGame1.setDescription("Description 1");
        testGame1.setReleaseDate(Date.valueOf(LocalDate.of(2023, 1, 1)));
        testGame1.setRating(4.5);
        testGame1.setLocalRating(4.2);
        testGame1.setLocalRatingCount(100);
        testGame1.setMetacriticRate(85.0);
        testGame1.setPlaytime(20);
        testGame1.setPosterUrl("http://test.com/poster1.jpg");
        testGame1.setPlatforms(Set.of(PlatformEnum.PC, PlatformEnum.PLAYSTATION));
        testGame1.setTrailerUrls(List.of("trailer1.mp4", "trailer2.mp4"));
        testGame1.setWalkthroughUrls(List.of("walkthrough1.mp4"));
        testGame1.setStoreLinks(List.of("http://store.com/game1"));
        testGame1.setScreenshotUrls(List.of("screenshot1.jpg", "screenshot2.jpg"));

        testGame2 = new GameEntity();
        testGame2.setId(TEST_GAME_ID_2);
        testGame2.setName("Test Game 2");
        testGame2.setDescription("Description 2");
        testGame2.setReleaseDate(Date.valueOf(LocalDate.of(2023, 2, 1)));
        testGame2.setRating(4.0);
        testGame2.setLocalRating(3.8);
        testGame2.setLocalRatingCount(50);
        testGame2.setPlaytime(15);
        testGame2.setPosterUrl("http://test.com/poster2.jpg");

        testTag = new TagEntity();
        testTag.setId(TEST_TAG_ID);
        testTag.setName("Action");
        testTag.setNameRu("Экшен");
        testTag.setSlug("action");

        testGameTag = new GameTag();
        testGameTag.setGame(testGame1);
        testGameTag.setTag(testTag);
        testGame1.setGameTags(Set.of(testGameTag));

        testGameDto1 = new GameDto();
        testGameDto1.setId(TEST_GAME_ID_1);
        testGameDto1.setName("Test Game 1");
        testGameDto1.setPosterUrl("http://test.com/poster1.jpg");
        testGameDto1.setLocalRating(4.2);

        testGameDto2 = new GameDto();
        testGameDto2.setId(TEST_GAME_ID_2);
        testGameDto2.setName("Test Game 2");
        testGameDto2.setPosterUrl("http://test.com/poster2.jpg");
        testGameDto2.setLocalRating(3.8);

        testGameDetailsDto = new GameDetailsDto();
        testGameDetailsDto.setId(TEST_GAME_ID_1);
        testGameDetailsDto.setName("Test Game 1");
        testGameDetailsDto.setDescription("Description 1");
        testGameDetailsDto.setReleaseDate(LocalDate.of(2023, 1, 1));
        testGameDetailsDto.setPosterUrl("http://test.com/poster1.jpg");
        testGameDetailsDto.setLocalRating(4.2);
        testGameDetailsDto.setLocalRatingCount(100);
        testGameDetailsDto.setRating(BigDecimal.valueOf(4.5));
        testGameDetailsDto.setMetacriticRate(BigDecimal.valueOf(85.0));
        testGameDetailsDto.setPlaytime(20);
        testGameDetailsDto.setPlatforms(List.of("PC", "PLAYSTATION"));
        testGameDetailsDto.setTrailerUrls(List.of("trailer1.mp4", "trailer2.mp4"));
        testGameDetailsDto.setWalkthroughUrls(List.of("walkthrough1.mp4"));
        testGameDetailsDto.setStoreLinks(List.of("http://store.com/game1"));
        testGameDetailsDto.setScreenshotUrls(List.of("screenshot1.jpg", "screenshot2.jpg"));

        testDeals = List.of(new PriceService.GameDeal("1", "10.99", "19.99", "45"), new PriceService.GameDeal("2", "15.99", "29.99", "47"), new PriceService.GameDeal("3", "20.99", "39.99", "48"));
        testUsdRate = 75.5;
    }

    @Test
    void getAll_ShouldReturnListOfGameDtos() {
        int page = 0;
        int size = 20;
        PageRequest pageable = PageRequest.of(page, size);
        List<GameEntity> games = Arrays.asList(testGame1, testGame2);
        Page<GameEntity> gamePage = new PageImpl<>(games, pageable, games.size());

        when(gameService.getGames(page, size)).thenReturn(gamePage);
        when(mapper.map(testGame1, GameDto.class)).thenReturn(testGameDto1);
        when(mapper.map(testGame2, GameDto.class)).thenReturn(testGameDto2);

        List<GameDto> result = gameController.getAll(page, size);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(TEST_GAME_ID_1, result.get(0).getId());
        assertEquals(TEST_GAME_ID_2, result.get(1).getId());
        
        verify(gameService).getGames(page, size);
        verify(mapper, times(2)).map(any(GameEntity.class), eq(GameDto.class));
    }

    @Test
    void getAll_ShouldUseDefaultPagination() {
        int defaultPage = 0;
        int defaultSize = 20;
        Page<GameEntity> emptyPage = new PageImpl<>(new ArrayList<>());
        
        when(gameService.getGames(defaultPage, defaultSize)).thenReturn(emptyPage);

        List<GameDto> result = gameController.getAll(defaultPage, defaultSize);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(gameService).getGames(defaultPage, defaultSize);
    }

    @Test
    void getAll_ShouldHandleEmptyResult() {
        int page = 0;
        int size = 20;
        Page<GameEntity> emptyPage = new PageImpl<>(new ArrayList<>());
        
        when(gameService.getGames(page, size)).thenReturn(emptyPage);

        List<GameDto> result = gameController.getAll(page, size);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAll_ShouldSetLocalRatingWhenNull() {
        int page = 0;
        int size = 20;
        PageRequest pageable = PageRequest.of(page, size);
        List<GameEntity> games = Collections.singletonList(testGame1);
        Page<GameEntity> gamePage = new PageImpl<>(games, pageable, games.size());
        
        GameDto dtoWithNullRating = new GameDto();
        dtoWithNullRating.setId(TEST_GAME_ID_1);
        dtoWithNullRating.setLocalRating(null);
        
        when(gameService.getGames(page, size)).thenReturn(gamePage);
        when(mapper.map(testGame1, GameDto.class)).thenReturn(dtoWithNullRating);

        List<GameDto> result = gameController.getAll(page, size);

        assertNotNull(result);
        assertEquals(4.2, result.get(0).getLocalRating());
    }

    @Test
    void loadGame_ShouldLoadGameAndReturnId() {
        when(gameService.loadGameIfNeeded(TEST_GAME_ID_1)).thenReturn(testGame1);

        ResponseEntity<Long> response = gameController.loadGame(TEST_GAME_ID_1);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(TEST_GAME_ID_1, response.getBody());
        
        verify(gameService).loadGameIfNeeded(TEST_GAME_ID_1);
    }

    @Test
    void loadGame_WhenGameNotFound_ShouldThrowException() {
        when(gameService.loadGameIfNeeded(TEST_GAME_ID_1)).thenThrow(new RuntimeException("Game not found"));

        assertThrows(RuntimeException.class, () -> gameController.loadGame(TEST_GAME_ID_1));
        verify(gameService).loadGameIfNeeded(TEST_GAME_ID_1);
    }


    @Test
    void filter_ShouldReturnFilteredGames() {
        String search = TEST_SEARCH_QUERY;
        int page = 0;
        int size = 20;
        PageRequest pageable = PageRequest.of(page, size);
        List<GameEntity> games = Collections.singletonList(testGame1);
        Page<GameEntity> gamePage = new PageImpl<>(games, pageable, games.size());
        
        when(gameService.getAllByFilters(search, page, size)).thenReturn(gamePage);
        when(mapper.map(testGame1, GameDto.class)).thenReturn(testGameDto1);

        
        List<GameDto> result = gameController.filter(search, page, size);

        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_GAME_ID_1, result.get(0).getId());
        
        verify(gameService).getAllByFilters(search, page, size);
    }

    @Test
    void filter_WithEmptySearch_ShouldReturnAllGames() {
        
        String search = "";
        int page = 0;
        int size = 20;
        PageRequest pageable = PageRequest.of(page, size);
        List<GameEntity> games = Arrays.asList(testGame1, testGame2);
        Page<GameEntity> gamePage = new PageImpl<>(games, pageable, games.size());
        
        when(gameService.getAllByFilters(search, page, size)).thenReturn(gamePage);
        when(mapper.map(testGame1, GameDto.class)).thenReturn(testGameDto1);
        when(mapper.map(testGame2, GameDto.class)).thenReturn(testGameDto2);

        
        List<GameDto> result = gameController.filter(search, page, size);

        
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getByTag_ShouldReturnGamesByTag() {
        
        int page = 0;
        int size = 20;
        PageRequest pageable = PageRequest.of(page, size);
        List<GameEntity> games = Collections.singletonList(testGame1);
        Page<GameEntity> gamePage = new PageImpl<>(games, pageable, games.size());
        
        when(gameService.getGamesByTag(TEST_TAG_ID, page, size)).thenReturn(gamePage);
        when(mapper.map(testGame1, GameDto.class)).thenReturn(testGameDto1);

        
        List<GameDto> result = gameController.getByTag(TEST_TAG_ID, page, size);

        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_GAME_ID_1, result.get(0).getId());
        
        verify(gameService).getGamesByTag(TEST_TAG_ID, page, size);
    }

    @Test
    void getByTag_WhenNoGames_ShouldReturnEmptyList() {
        
        int page = 0;
        int size = 20;
        PageRequest pageable = PageRequest.of(page, size);
        Page<GameEntity> emptyPage = new PageImpl<>(new ArrayList<>());
        
        when(gameService.getGamesByTag(TEST_TAG_ID, page, size)).thenReturn(emptyPage);

        
        List<GameDto> result = gameController.getByTag(TEST_TAG_ID, page, size);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void favorites_ShouldReturnFavoriteGames() {
        
        List<GameEntity> favoriteGames = Arrays.asList(testGame1, testGame2);
        
        when(gameService.getFavorites(TEST_USER_ID)).thenReturn(favoriteGames);
        when(mapper.map(testGame1, GameDto.class)).thenReturn(testGameDto1);
        when(mapper.map(testGame2, GameDto.class)).thenReturn(testGameDto2);

        
        List<GameDto> result = gameController.favorites(TEST_USER_ID);

        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(TEST_GAME_ID_1, result.get(0).getId());
        assertEquals(TEST_GAME_ID_2, result.get(1).getId());
        
        verify(gameService).getFavorites(TEST_USER_ID);
    }

    @Test
    void favorites_WhenNoFavorites_ShouldReturnEmptyList() {
        
        when(gameService.getFavorites(TEST_USER_ID)).thenReturn(new ArrayList<>());

        
        List<GameDto> result = gameController.favorites(TEST_USER_ID);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void popular_ShouldReturnPopularGames() {
        
        int page = 0;
        int size = 20;
        PageRequest pageable = PageRequest.of(page, size);
        List<GameEntity> popularGames = Arrays.asList(testGame1, testGame2);
        Page<GameEntity> gamePage = new PageImpl<>(popularGames, pageable, popularGames.size());
        
        when(gameService.getPopularGames(page, size)).thenReturn(gamePage);
        when(mapper.map(testGame1, GameDto.class)).thenReturn(testGameDto1);
        when(mapper.map(testGame2, GameDto.class)).thenReturn(testGameDto2);

        
        List<GameDto> result = gameController.popular(page, size);

        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        verify(gameService).getPopularGames(page, size);
    }

    @Test
    void popular_ShouldUseDefaultPagination() {
        
        int defaultPage = 0;
        int defaultSize = 20;
        Page<GameEntity> emptyPage = new PageImpl<>(new ArrayList<>());
        
        when(gameService.getPopularGames(defaultPage, defaultSize)).thenReturn(emptyPage);

        
        List<GameDto> result = gameController.popular(defaultPage, defaultSize);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    
    @Test
    void toDetailsDto_ShouldHandleNullFields() {
        
        GameEntity gameWithNulls = new GameEntity();
        gameWithNulls.setId(TEST_GAME_ID_1);
        gameWithNulls.setName("Test Game");
        gameWithNulls.setDescription(null);
        gameWithNulls.setReleaseDate(null);
        gameWithNulls.setPlatforms(null);
        gameWithNulls.setRating(null);
        gameWithNulls.setMetacriticRate(null);
        gameWithNulls.setGameTags(null);
        
        when(gameService.getGame(TEST_GAME_ID_1)).thenReturn(gameWithNulls);
        when(priceService.getBestDeals(anyString())).thenReturn(new ArrayList<>());
        when(newsService.getLatestNews(anyString())).thenReturn(new ArrayList<>());
        when(currencyService.getUsdRate()).thenReturn(testUsdRate);

        
        ResponseEntity<GameDetailsDto> response = gameController.get(TEST_GAME_ID_1);

        
        assertNotNull(response);
        GameDetailsDto result = response.getBody();
        assertNotNull(result);
        assertNull(result.getDescription());
        assertNull(result.getReleaseDate());
        assertNull(result.getPlatforms());
        assertNull(result.getRating());
        assertNull(result.getMetacriticRate());
        assertNull(result.getTags());
    }

}
