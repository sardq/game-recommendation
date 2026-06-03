package com.diplome.game_recommendation.controllers;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.diplome.game_recommendation.dtos.RecommendationDto;
import com.diplome.game_recommendation.dtos.RecommendationSessionDetailsDto;
import com.diplome.game_recommendation.dtos.RecommendationSessionDto;
import com.diplome.game_recommendation.helpers.configuration.*;
import com.diplome.game_recommendation.services.RecomendationService;;
@RestController
@RequestMapping(Constants.API_URL + "/recommendations")
public class RecomendationController {

    private final RecomendationService service;


    public RecomendationController(RecomendationService service) {
        this.service = service;
    }

        @GetMapping("/{userId}")
        public List<RecommendationDto> get(@PathVariable Long userId) {
            return service.getRecommendationsForUser(userId);
        }
        @GetMapping("/user")
        public List<RecommendationDto> get(Authentication authentication) {
            return service.getFastRecommendations(authentication);
        }
        @PostMapping("/recalculate/{userId}")
    public void recalculatePreferences(@PathVariable Long userId) {
        service.recalculateUserPreferences(userId);
    }
    @PostMapping("/recalculate")
    public void recalculatePreferencesAuth(Authentication authentication) {
        service.generateAndSaveRecommendations(authentication);
    }
    @GetMapping("/sessions/user")
    public List<RecommendationSessionDto> getSessions(Authentication authentication) {
        return service.getUserSessions(authentication);
    }

    @GetMapping("/session/{sessionId}")
    public RecommendationSessionDetailsDto getSession(@PathVariable Long sessionId) {
        return service.getSessionDetails(sessionId);
    }
}
