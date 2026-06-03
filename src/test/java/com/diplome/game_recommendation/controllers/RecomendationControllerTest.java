package com.diplome.game_recommendation.controllers;

import com.diplome.game_recommendation.dtos.RecommendationDto;
import com.diplome.game_recommendation.dtos.RecommendationSessionDetailsDto;
import com.diplome.game_recommendation.dtos.RecommendationSessionDto;
import com.diplome.game_recommendation.helpers.configuration.Constants;
import com.diplome.game_recommendation.services.RecomendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecomendationControllerTest {

    @Mock
    private RecomendationService recomendationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RecomendationController recomendationController;

    private final Long TEST_USER_ID = 100L;
    private final Long TEST_SESSION_ID = 500L;
    private final String TEST_EMAIL = "test@example.com";

    private RecommendationDto testRecommendation1;
    private RecommendationDto testRecommendation2;
    private RecommendationSessionDto testSessionDto;
    private RecommendationSessionDetailsDto testSessionDetailsDto;
    private List<RecommendationDto> testRecommendations;
    private List<RecommendationSessionDto> testSessions;

    @BeforeEach
    void setUp() {
        testRecommendation1 = new RecommendationDto();
        testRecommendation1.setGameId(1L);
        testRecommendation1.setName("Game 1");
        testRecommendation1.setPosterUrl("http://test.com/game1.jpg");
        testRecommendation1.setRecommendationScore(0.95);
        testRecommendation1.setMatchPercentage(95);
        testRecommendation1.setRating(4.5);
        testRecommendation1.setLocalRating(4.3);

        testRecommendation2 = new RecommendationDto();
        testRecommendation2.setGameId(2L);
        testRecommendation2.setName("Game 2");
        testRecommendation2.setPosterUrl("http://test.com/game2.jpg");
        testRecommendation2.setRecommendationScore(0.85);
        testRecommendation2.setMatchPercentage(85);
        testRecommendation2.setRating(4.2);
        testRecommendation2.setLocalRating(4.0);

        testRecommendations = Arrays.asList(testRecommendation1, testRecommendation2);

        testSessionDto = new RecommendationSessionDto();
        testSessionDto.setId(TEST_SESSION_ID);
        testSessionDto.setGeneratedAt(LocalDateTime.now());
        testSessionDto.setItemsCount(10);

        testSessions = Arrays.asList(testSessionDto);

        testSessionDetailsDto = new RecommendationSessionDetailsDto();
        testSessionDetailsDto.setId(TEST_SESSION_ID);
        testSessionDetailsDto.setGeneratedAt(LocalDateTime.now());
        testSessionDetailsDto.setItems(testRecommendations);
    }

    @Test
    void get_ByUserId_ShouldReturnRecommendations() {
        
        when(recomendationService.getRecommendationsForUser(TEST_USER_ID))
            .thenReturn(testRecommendations);

        
        List<RecommendationDto> result = recomendationController.get(TEST_USER_ID);

        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getGameId());
        assertEquals("Game 1", result.get(0).getName());
        assertEquals(0.95, result.get(0).getRecommendationScore());
        assertEquals(95, result.get(0).getMatchPercentage());
        
        assertEquals(2L, result.get(1).getGameId());
        assertEquals("Game 2", result.get(1).getName());
        
        verify(recomendationService).getRecommendationsForUser(TEST_USER_ID);
    }

    @Test
    void get_ByUserId_WhenNoRecommendations_ShouldReturnEmptyList() {
        
        when(recomendationService.getRecommendationsForUser(TEST_USER_ID))
            .thenReturn(List.of());

        
        List<RecommendationDto> result = recomendationController.get(TEST_USER_ID);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(recomendationService).getRecommendationsForUser(TEST_USER_ID);
    }

    @Test
    void get_ByAuthentication_ShouldReturnFastRecommendations() {
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(recomendationService.getFastRecommendations(authentication))
            .thenReturn(testRecommendations);

        
        List<RecommendationDto> result = recomendationController.get(authentication);

        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getGameId());
        assertEquals(2L, result.get(1).getGameId());
        
        verify(recomendationService).getFastRecommendations(authentication);
    }

    @Test
    void get_ByAuthentication_WhenNoFastRecommendations_ShouldReturnColdStart() {
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(recomendationService.getFastRecommendations(authentication))
            .thenReturn(List.of());

        
        List<RecommendationDto> result = recomendationController.get(authentication);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(recomendationService).getFastRecommendations(authentication);
    }

    @Test
    void recalculatePreferences_ByUserId_ShouldRecalculate() {
        
        doNothing().when(recomendationService).recalculateUserPreferences(TEST_USER_ID);

        
        recomendationController.recalculatePreferences(TEST_USER_ID);

        
        verify(recomendationService).recalculateUserPreferences(TEST_USER_ID);
    }

    @Test
    void recalculatePreferences_ByAuthentication_ShouldGenerateAndSave() {
        
        doNothing().when(recomendationService).generateAndSaveRecommendations(authentication);

        
        recomendationController.recalculatePreferencesAuth(authentication);

        
        verify(recomendationService).generateAndSaveRecommendations(authentication);
    }

    @Test
    void getSessions_ShouldReturnUserSessions() {
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(recomendationService.getUserSessions(authentication))
            .thenReturn(testSessions);

        
        List<RecommendationSessionDto> result = recomendationController.getSessions(authentication);

        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_SESSION_ID, result.get(0).getId());
        assertEquals(10, result.get(0).getItemsCount());
        assertNotNull(result.get(0).getGeneratedAt());
        
        verify(recomendationService).getUserSessions(authentication);
    }

    @Test
    void getSessions_WhenNoSessions_ShouldReturnEmptyList() {
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(recomendationService.getUserSessions(authentication))
            .thenReturn(List.of());

        
        List<RecommendationSessionDto> result = recomendationController.getSessions(authentication);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(recomendationService).getUserSessions(authentication);
    }

    @Test
    void getSession_ShouldReturnSessionDetails() {
        
        when(recomendationService.getSessionDetails(TEST_SESSION_ID))
            .thenReturn(testSessionDetailsDto);

        
        RecommendationSessionDetailsDto result = recomendationController.getSession(TEST_SESSION_ID);

        
        assertNotNull(result);
        assertEquals(TEST_SESSION_ID, result.getId());
        assertEquals(2, result.getItems().size());
        assertEquals(1L, result.getItems().get(0).getGameId());
        assertEquals(2L, result.getItems().get(1).getGameId());
        assertNotNull(result.getGeneratedAt());
        
        verify(recomendationService).getSessionDetails(TEST_SESSION_ID);
    }

    @Test
    void getSession_WhenSessionNotFound_ShouldThrowException() {
        
        when(recomendationService.getSessionDetails(TEST_SESSION_ID))
            .thenThrow(new RuntimeException("Session not found"));

        assertThrows(RuntimeException.class, 
            () -> recomendationController.getSession(TEST_SESSION_ID));
        
        verify(recomendationService).getSessionDetails(TEST_SESSION_ID);
    }

    @Test
    void get_ByUserId_ShouldHandleLargeUserId() {
        
        Long largeUserId = 999999999L;
        when(recomendationService.getRecommendationsForUser(largeUserId))
            .thenReturn(testRecommendations);

        
        List<RecommendationDto> result = recomendationController.get(largeUserId);

        
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(recomendationService).getRecommendationsForUser(largeUserId);
    }

    @Test
    void recalculatePreferences_ByUserId_ShouldHandleInvalidUserId() {
        
        Long invalidUserId = -1L;
        doThrow(new RuntimeException("User not found"))
            .when(recomendationService).recalculateUserPreferences(invalidUserId);

        assertThrows(RuntimeException.class, 
            () -> recomendationController.recalculatePreferences(invalidUserId));
        
        verify(recomendationService).recalculateUserPreferences(invalidUserId);
    }


    @Test
    void get_ByUserId_ShouldHandleZeroRecommendations() {
        
        when(recomendationService.getRecommendationsForUser(TEST_USER_ID))
            .thenReturn(List.of());

        
        List<RecommendationDto> result = recomendationController.get(TEST_USER_ID);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void get_ByUserId_ShouldPreserveRecommendationOrder() {
        
        when(recomendationService.getRecommendationsForUser(TEST_USER_ID))
            .thenReturn(testRecommendations);

        
        List<RecommendationDto> result = recomendationController.get(TEST_USER_ID);

        
        assertNotNull(result);
        assertEquals(0.95, result.get(0).getRecommendationScore());
        assertEquals(0.85, result.get(1).getRecommendationScore());
        assertTrue(result.get(0).getRecommendationScore() > result.get(1).getRecommendationScore());
    }

    @Test
    void getSession_ShouldReturnDetailsWithCorrectStructure() {
        
        RecommendationSessionDetailsDto detailedDto = new RecommendationSessionDetailsDto();
        detailedDto.setId(TEST_SESSION_ID);
        detailedDto.setGeneratedAt(LocalDateTime.now());
        detailedDto.setItems(testRecommendations);
        
        when(recomendationService.getSessionDetails(TEST_SESSION_ID))
            .thenReturn(detailedDto);

        
        RecommendationSessionDetailsDto result = recomendationController.getSession(TEST_SESSION_ID);

        
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertFalse(result.getItems().isEmpty());
        
        for (RecommendationDto item : result.getItems()) {
            assertNotNull(item.getGameId());
            assertNotNull(item.getName());
            assertNotNull(item.getRecommendationScore());
            assertNotNull(item.getMatchPercentage());
        }
    }

    @Test
    void recalculatePreferences_ByAuthentication_ShouldLogCorrectly() {
        
        doNothing().when(recomendationService).generateAndSaveRecommendations(authentication);

        
        recomendationController.recalculatePreferencesAuth(authentication);

        
        verify(recomendationService).generateAndSaveRecommendations(authentication);
    }

}