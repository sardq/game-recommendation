package com.diplome.game_recommendation.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(RecomendationController.class);

    public RecomendationController(RecomendationService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public List<RecommendationDto> get(@PathVariable Long userId) {
        logger.info("Получение рекомендаций userId={}", userId);
        return service.getRecommendationsForUser(userId);
    }

    @PostMapping("/generate/{userId}")
    public void generate(@PathVariable Long userId) {
        logger.info("Генерация рекомендаций userId={}", userId);
        service.generateRecommendationSession(userId);
    }

    @GetMapping("/sessions/{userId}")
    public List<RecommendationSessionDto> sessions(@PathVariable Long userId) {
        logger.info("Сессии рекомендаций userId={}", userId);
        return service.getUserSessions(userId);
    }

    @GetMapping("/session/{sessionId}")
    public RecommendationSessionDetailsDto session(@PathVariable Long sessionId) {
        logger.info("Детали сессии sessionId={}", sessionId);
        return service.getSession(sessionId);
    }

    @GetMapping("/similar/{gameId}")
    public List<RecommendationDto> similar(@PathVariable Long gameId) {
        logger.info("Похожие игры gameId={}", gameId);
        return service.getSimilarGames(gameId);
    }
}
