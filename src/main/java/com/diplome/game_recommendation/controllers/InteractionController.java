package com.diplome.game_recommendation.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.diplome.game_recommendation.core.configuration.*;
import com.diplome.game_recommendation.services.InteractionService;
@RestController
@RequestMapping(Constants.API_URL + "/interactions")
public class InteractionController {

    private final InteractionService interactionService;

    private static final Logger logger = LoggerFactory.getLogger(InteractionController.class);

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/view")
    public void view(@RequestParam Long userId,
                     @RequestParam Long gameId) {

        logger.info("Просмотр игры userId={}, gameId={}", userId, gameId);
        interactionService.recordView(userId, gameId);
    }

    @PostMapping("/rate")
    public void rate(@RequestParam Long userId,
                     @RequestParam Long gameId,
                     @RequestParam Integer rating) {

        logger.info("Оценка игры userId={}, gameId={}, rating={}", userId, gameId, rating);
        interactionService.recordRating(userId, gameId, rating);
    }

    @PostMapping("/favorite")
    public void addFavorite(@RequestParam Long userId,
                            @RequestParam Long gameId) {

        logger.info("Добавление в избранное");
        interactionService.addToFavorites(userId, gameId);
    }

    @DeleteMapping("/favorite")
    public void removeFavorite(@RequestParam Long userId,
                               @RequestParam Long gameId) {

        logger.info("Удаление из избранного");
        interactionService.removeFromFavorites(userId, gameId);
    }

    @GetMapping("/favorite")
    public boolean isFavorite(@RequestParam Long userId,
                              @RequestParam Long gameId) {

        return interactionService.isFavorite(userId, gameId);
    }
}
