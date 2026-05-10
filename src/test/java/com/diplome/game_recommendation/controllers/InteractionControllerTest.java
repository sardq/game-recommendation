package com.diplome.game_recommendation.controllers;

import com.diplome.game_recommendation.dtos.GameDto;
import com.diplome.game_recommendation.dtos.InteractionDto;
import com.diplome.game_recommendation.dtos.ReviewDto;
import com.diplome.game_recommendation.helpers.configuration.Constants;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.InteractionEnum;
import com.diplome.game_recommendation.models.ReactionType;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.services.InteractionService;
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
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionControllerTest {

    @Mock
    private InteractionService interactionService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InteractionController interactionController;

    private final Long TEST_GAME_ID = 100L;
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_REVIEW_ID = 500L;
    private final Integer TEST_RATING = 5;
    private final String TEST_REVIEW_TEXT = "Great game!";
    private final String TEST_REACTION_TYPE = "LIKE";
    private final String TEST_USERNAME = "testuser";

    private ReviewDto testReviewDto;
    private GameDto testGameDto;
    private InteractionDto testInteractionDto;
    private UserGames testUserGames;
    private GameEntity testGameEntity;

    @BeforeEach
    void setUp() {
        // Setup ReviewDto
        testReviewDto = new ReviewDto();
        testReviewDto.setId(TEST_REVIEW_ID);
        testReviewDto.setLogin(TEST_USERNAME);
        testReviewDto.setReview(TEST_REVIEW_TEXT);
        testReviewDto.setRating(TEST_RATING);
        testReviewDto.setLikesCount(5L);
        testReviewDto.setDislikesCount(2L);
        testReviewDto.setFunnyCount(1L);

        // Setup GameDto
        testGameDto = new GameDto();
        testGameDto.setId(TEST_GAME_ID);
        testGameDto.setName("Test Game");
        testGameDto.setPosterUrl("http://test.com/poster.jpg");

        // Setup InteractionDto
        testInteractionDto = new InteractionDto();
        testInteractionDto.setInteractionType(InteractionEnum.Favorite.toString());
        testInteractionDto.setRating(TEST_RATING);

        // Setup UserGames entity
        testUserGames = new UserGames();
        testUserGames.setId(1L);
        testUserGames.setInteraction(InteractionEnum.Favorite);
        testUserGames.setRating(TEST_RATING);
        testUserGames.setReview(TEST_REVIEW_TEXT);
        testUserGames.setTime(LocalDateTime.now());

        // Setup GameEntity
        testGameEntity = new GameEntity();
        testGameEntity.setId(TEST_GAME_ID);
        testGameEntity.setName("Test Game");
        testGameEntity.setPosterUrl("http://test.com/poster.jpg");
    }

    @Test
    void view_ShouldRecordView() {
        // Act
        interactionController.view(authentication, TEST_GAME_ID);

        // Assert
        verify(interactionService).recordView(authentication, TEST_GAME_ID);
    }

    @Test
    void rate_ShouldRecordRating() {
        // Act
        interactionController.rate(authentication, TEST_GAME_ID, TEST_RATING);

        // Assert
        verify(interactionService).recordRating(authentication, TEST_GAME_ID, TEST_RATING);
    }

    @Test
    void addFavorite_ShouldAddToFavorites() {
        // Act
        interactionController.addFavorite(authentication, TEST_GAME_ID);

        // Assert
        verify(interactionService).addToFavorites(authentication, TEST_GAME_ID);
    }

    @Test
    void removeFavorite_ShouldRemoveFromFavorites() {
        // Act
        interactionController.removeFavorite(authentication, TEST_GAME_ID);

        // Assert
        verify(interactionService).removeFromFavorites(authentication, TEST_GAME_ID);
    }

    @Test
    void isFavorite_ShouldReturnTrue() {
        // Arrange
        when(interactionService.isFavorite(authentication, TEST_GAME_ID)).thenReturn(true);

        // Act
        boolean result = interactionController.isFavorite(authentication, TEST_GAME_ID);

        // Assert
        assertTrue(result);
        verify(interactionService).isFavorite(authentication, TEST_GAME_ID);
    }

    @Test
    void isFavorite_ShouldReturnFalse() {
        // Arrange
        when(interactionService.isFavorite(authentication, TEST_GAME_ID)).thenReturn(false);

        // Act
        boolean result = interactionController.isFavorite(authentication, TEST_GAME_ID);

        // Assert
        assertFalse(result);
        verify(interactionService).isFavorite(authentication, TEST_GAME_ID);
    }

    @Test
    void getReviews_ShouldReturnListOfReviews() {
        // Arrange
        int page = 0;
        int size = 10;
        List<ReviewDto> reviews = Arrays.asList(testReviewDto);
        
        when(interactionService.getReviewsByGame(TEST_GAME_ID, page, size, authentication))
            .thenReturn(reviews);

        // Act
        List<ReviewDto> result = interactionController.getReviews(TEST_GAME_ID, page, size, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_REVIEW_ID, result.get(0).getId());
        assertEquals(TEST_REVIEW_TEXT, result.get(0).getReview());
        
        verify(interactionService).getReviewsByGame(TEST_GAME_ID, page, size, authentication);
    }

    @Test
    void getReviews_WithEmptyList_ShouldReturnEmptyList() {
        // Arrange
        int page = 0;
        int size = 10;
        
        when(interactionService.getReviewsByGame(TEST_GAME_ID, page, size, authentication))
            .thenReturn(List.of());

        // Act
        List<ReviewDto> result = interactionController.getReviews(TEST_GAME_ID, page, size, authentication);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getReviewsByUser_ShouldReturnPaginatedReviews() {
        // Arrange
        int page = 0;
        int size = 5;
        List<ReviewDto> reviews = Arrays.asList(testReviewDto);
        
        when(interactionService.getReviewsByUserIdPaginated(TEST_USER_ID, page, size))
            .thenReturn(reviews);

        // Act
        List<ReviewDto> result = interactionController.getReviewsByUser(TEST_USER_ID, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        verify(interactionService).getReviewsByUserIdPaginated(TEST_USER_ID, page, size);
    }

    @Test
    void getReviewsByUser_ShouldUseDefaultPagination() {
        // Arrange
        int defaultPage = 0;
        int defaultSize = 5;
        
        when(interactionService.getReviewsByUserIdPaginated(TEST_USER_ID, defaultPage, defaultSize))
            .thenReturn(List.of());

        // Act
        List<ReviewDto> result = interactionController.getReviewsByUser(TEST_USER_ID, defaultPage, defaultSize);

        // Assert
        assertNotNull(result);
        verify(interactionService).getReviewsByUserIdPaginated(TEST_USER_ID, defaultPage, defaultSize);
    }

    @Test
    void getUserReview_ShouldReturnUserReview() {
        // Arrange
        when(interactionService.getUserReview(TEST_GAME_ID, authentication))
            .thenReturn(testReviewDto);

        // Act
        ReviewDto result = interactionController.getUserReview(TEST_GAME_ID, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_REVIEW_ID, result.getId());
        assertEquals(TEST_REVIEW_TEXT, result.getReview());
        
        verify(interactionService).getUserReview(TEST_GAME_ID, authentication);
    }

    @Test
    void getUserReview_WhenNoReview_ShouldReturnNull() {
        // Arrange
        when(interactionService.getUserReview(TEST_GAME_ID, authentication))
            .thenReturn(null);

        // Act
        ReviewDto result = interactionController.getUserReview(TEST_GAME_ID, authentication);

        // Assert
        assertNull(result);
    }

    @Test
    void review_ShouldAddReview() {
        // Act
        interactionController.review(authentication, TEST_GAME_ID, TEST_REVIEW_TEXT);

        // Assert
        verify(interactionService).addReview(authentication, TEST_GAME_ID, TEST_REVIEW_TEXT);
    }

    @Test
    void getUserInteraction_WhenExists_ShouldReturnInteractionDto() {
        // Arrange
        String type = "Favorite";
        
        when(interactionService.getUserInteraction(authentication, TEST_GAME_ID, type))
            .thenReturn(Optional.of(testUserGames));
        when(modelMapper.map(testUserGames, InteractionDto.class)).thenReturn(testInteractionDto);

        // Act
        InteractionDto result = interactionController.getUserInteraction(authentication, TEST_GAME_ID, type);

        // Assert
        assertNotNull(result);
        assertEquals(InteractionEnum.Favorite.toString(), result.getInteractionType());
        assertEquals(TEST_RATING, result.getRating());
        
        verify(interactionService).getUserInteraction(authentication, TEST_GAME_ID, type);
        verify(modelMapper).map(testUserGames, InteractionDto.class);
    }

    @Test
    void getUserInteraction_WhenNotExists_ShouldReturnNull() {
        // Arrange
        String type = "Favorite";
        
        when(interactionService.getUserInteraction(authentication, TEST_GAME_ID, type))
            .thenReturn(Optional.empty());

        // Act
        InteractionDto result = interactionController.getUserInteraction(authentication, TEST_GAME_ID, type);

        // Assert
        assertNull(result);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void getUserFavorites_ShouldReturnListOfGameDtos() {
        // Arrange
        List<GameDto> favorites = Arrays.asList(testGameDto);
        
        when(interactionService.getUserFavorites(authentication)).thenReturn(favorites);

        // Act
        List<GameDto> result = interactionController.getUserFavorites(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_GAME_ID, result.get(0).getId());
        
        verify(interactionService).getUserFavorites(authentication);
    }

    @Test
    void getUserFavorites_WhenNoFavorites_ShouldReturnEmptyList() {
        // Arrange
        when(interactionService.getUserFavorites(authentication)).thenReturn(List.of());

        // Act
        List<GameDto> result = interactionController.getUserFavorites(authentication);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getFilteredFavorites_ShouldReturnPageOfGameDtos() {
        // Arrange
        String search = "game";
        Long tagId = 10L;
        int page = 0;
        PageRequest pageable = PageRequest.of(page, 10);
        
        List<GameEntity> games = Arrays.asList(testGameEntity);
        Page<GameEntity> gamePage = new PageImpl<>(games, pageable, games.size());
        
        when(interactionService.getFavoritesFiltered(authentication, search, tagId, pageable))
            .thenReturn(gamePage);
        when(modelMapper.map(testGameEntity, GameDto.class)).thenReturn(testGameDto);

        // Act
        Page<GameDto> result = interactionController.getFilteredFavorites(authentication, search, tagId, page);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(TEST_GAME_ID, result.getContent().get(0).getId());
        
        verify(interactionService).getFavoritesFiltered(authentication, search, tagId, pageable);
        verify(modelMapper).map(testGameEntity, GameDto.class);
    }

    @Test
    void getFilteredFavorites_WithNullSearchAndTagId_ShouldReturnAll() {
        // Arrange
        String search = null;
        Long tagId = null;
        int page = 0;
        PageRequest pageable = PageRequest.of(page, 10);
        
        Page<GameEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(interactionService.getFavoritesFiltered(authentication, search, tagId, pageable))
            .thenReturn(emptyPage);

        // Act
        Page<GameDto> result = interactionController.getFilteredFavorites(authentication, search, tagId, page);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getMyReviews_ShouldReturnUserReviews() {
        // Arrange
        int page = 0;
        int size = 5;
        List<ReviewDto> reviews = Arrays.asList(testReviewDto);
        
        when(interactionService.getUserReviews(authentication, page, size))
            .thenReturn(reviews);

        // Act
        List<ReviewDto> result = interactionController.getMyReviews(authentication, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        verify(interactionService).getUserReviews(authentication, page, size);
    }

    @Test
    void getMyReviews_ShouldUseDefaultPagination() {
        // Arrange
        int defaultPage = 0;
        int defaultSize = 5;
        
        when(interactionService.getUserReviews(authentication, defaultPage, defaultSize))
            .thenReturn(List.of());

        // Act
        List<ReviewDto> result = interactionController.getMyReviews(authentication, defaultPage, defaultSize);

        // Assert
        assertNotNull(result);
        verify(interactionService).getUserReviews(authentication, defaultPage, defaultSize);
    }

    @Test
    void reactToReview_ShouldReturnOkResponse() {
        // Arrange
        doNothing().when(interactionService).reactToReview(authentication, TEST_REVIEW_ID, TEST_REACTION_TYPE);

        // Act
        ResponseEntity<?> response = interactionController.reactToReview(TEST_REVIEW_ID, TEST_REACTION_TYPE, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        verify(interactionService).reactToReview(authentication, TEST_REVIEW_ID, TEST_REACTION_TYPE);
    }

    @Test
    void reactToReview_WithDifferentReactionTypes() {
        // Arrange
        String[] reactionTypes = {"LIKE", "DISLIKE", "FUNNY"};
        
        for (String type : reactionTypes) {
            doNothing().when(interactionService).reactToReview(authentication, TEST_REVIEW_ID, type);
            
            // Act
            ResponseEntity<?> response = interactionController.reactToReview(TEST_REVIEW_ID, type, authentication);
            
            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
        
        verify(interactionService, times(3)).reactToReview(eq(authentication), eq(TEST_REVIEW_ID), anyString());
    }


    @Test
    void rate_ShouldHandleInvalidRating() {
        // Arrange
        Integer invalidRating = 10;
        
        // Act
        interactionController.rate(authentication, TEST_GAME_ID, invalidRating);
        
        // Assert
        verify(interactionService).recordRating(authentication, TEST_GAME_ID, invalidRating);
    }

    @Test
    void review_WithEmptyReviewText_ShouldStillCallService() {
        // Arrange
        String emptyReview = "";
        
        // Act
        interactionController.review(authentication, TEST_GAME_ID, emptyReview);
        
        // Assert
        verify(interactionService).addReview(authentication, TEST_GAME_ID, emptyReview);
    }

    @Test
    void getReviews_WithLargePageSize_ShouldHandleCorrectly() {
        // Arrange
        int page = 0;
        int size = 1000;
        
        when(interactionService.getReviewsByGame(TEST_GAME_ID, page, size, authentication))
            .thenReturn(List.of());

        // Act
        List<ReviewDto> result = interactionController.getReviews(TEST_GAME_ID, page, size, authentication);

        // Assert
        assertNotNull(result);
        verify(interactionService).getReviewsByGame(TEST_GAME_ID, page, size, authentication);
    }

    @Test
    void getFilteredFavorites_WithCustomPageSize_ShouldUseCorrectPageRequest() {
        // Arrange
        String search = "action";
        Long tagId = 5L;
        int page = 2;
        
        Page<GameEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(page, 10), 0);
        
        when(interactionService.getFavoritesFiltered(eq(authentication), eq(search), eq(tagId), any(PageRequest.class)))
            .thenReturn(emptyPage);

        // Act
        Page<GameDto> result = interactionController.getFilteredFavorites(authentication, search, tagId, page);

        // Assert
        assertNotNull(result);
        verify(interactionService).getFavoritesFiltered(eq(authentication), eq(search), eq(tagId), argThat(req -> 
            req.getPageNumber() == page && req.getPageSize() == 10
        ));
    }

    @Test
    void toInteractionDto_ShouldMapCorrectly() {
        // This tests the private toDto method indirectly through getUserInteraction
        // Arrange
        String type = "Favorite";
        
        when(interactionService.getUserInteraction(authentication, TEST_GAME_ID, type))
            .thenReturn(Optional.of(testUserGames));
        when(modelMapper.map(testUserGames, InteractionDto.class)).thenReturn(testInteractionDto);

        // Act
        InteractionDto result = interactionController.getUserInteraction(authentication, TEST_GAME_ID, type);

        // Assert
        assertNotNull(result);
        verify(modelMapper).map(testUserGames, InteractionDto.class);
    }

    @Test
    void toGameDto_ShouldMapCorrectly() {
        // This tests the private toGamesDto method indirectly through getFilteredFavorites
        // Arrange
        String search = null;
        Long tagId = null;
        int page = 0;
        PageRequest pageable = PageRequest.of(page, 10);
        
        List<GameEntity> games = Arrays.asList(testGameEntity);
        Page<GameEntity> gamePage = new PageImpl<>(games, pageable, games.size());
        
        when(interactionService.getFavoritesFiltered(authentication, search, tagId, pageable))
            .thenReturn(gamePage);
        when(modelMapper.map(testGameEntity, GameDto.class)).thenReturn(testGameDto);

        // Act
        Page<GameDto> result = interactionController.getFilteredFavorites(authentication, search, tagId, page);

        // Assert
        assertNotNull(result);
        verify(modelMapper).map(testGameEntity, GameDto.class);
    }

}