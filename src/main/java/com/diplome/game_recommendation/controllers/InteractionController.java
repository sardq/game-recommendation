package com.diplome.game_recommendation.controllers;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.diplome.game_recommendation.dtos.GameDto;
import com.diplome.game_recommendation.dtos.InteractionDto;
import com.diplome.game_recommendation.dtos.ReviewDto;
import com.diplome.game_recommendation.helpers.configuration.*;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.services.InteractionService;
@RestController
@RequestMapping(Constants.API_URL + "/interactions")
public class InteractionController {

    private final InteractionService interactionService;
    private final ModelMapper modelMapper;
    private static final Logger logger = LoggerFactory.getLogger(InteractionController.class);

    public InteractionController(InteractionService interactionService, ModelMapper modelMapper) {
        this.interactionService = interactionService;
        this.modelMapper = modelMapper;
    }

    @PostMapping("/view")
    public void view(Authentication authentication,
                     @RequestParam Long gameId) {
        
        logger.info("Просмотр игры gameId={}", gameId);
        interactionService.recordView(authentication, gameId);
    }

    @PostMapping("/rate")
    public void rate(Authentication authentication,
                     @RequestParam Long gameId,
                     @RequestParam Integer rating) {

        logger.info("Оценка игры gameId={}, rating={}", gameId, rating);
        interactionService.recordRating(authentication, gameId, rating);
    }

    @PostMapping("/favorite")
    public void addFavorite(Authentication authentication,
                            @RequestParam Long gameId) {

        logger.info("Добавление в избранное");
        interactionService.addToFavorites(authentication, gameId);
    }

    @DeleteMapping("/favorite")
    public void removeFavorite(Authentication authentication,
                               @RequestParam Long gameId) {

        logger.info("Удаление из избранного");
        interactionService.removeFromFavorites(authentication, gameId);
    }

    @GetMapping("/favorite")
    public boolean isFavorite(Authentication authentication,
                              @RequestParam Long gameId) {

        return interactionService.isFavorite(authentication, gameId);
    }

    @GetMapping("/reviews/game/{gameId}")
    public List<ReviewDto> getReviews(@PathVariable Long gameId, @RequestParam int page, @RequestParam int size, Authentication authentication) {
        return interactionService.getReviewsByGame(gameId, page, size, authentication );
    }
    @GetMapping("/reviews/users/{userId}")
    public List<ReviewDto> getReviewsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        
        return interactionService.getReviewsByUserIdPaginated(userId, page, size);
    }
    @GetMapping("/review/user/{gameId}")
    public ReviewDto getUserReview(@PathVariable Long gameId, Authentication authentication) {
        return interactionService.getUserReview(gameId,authentication);
    }
    @PostMapping("/review")
    public void review(Authentication authentication,
                    @RequestParam Long gameId,
                    @RequestParam String review) {

        interactionService.addReview(authentication, gameId, review);
    }
    private InteractionDto toDto(UserGames entity) {
        return modelMapper.map(entity, InteractionDto.class);
    }
    private GameDto toGamesDto(GameEntity entity) {
        return modelMapper.map(entity, GameDto.class);
    }
    @GetMapping("/user/game/{gameId}")
    public InteractionDto getUserInteraction(
            Authentication authentication,
            @PathVariable Long gameId,
            @RequestParam String type) {

        UserGames ug = interactionService.getUserInteraction(authentication, gameId, type)
                .orElse(null);

        if (ug == null) return null;

        return toDto(ug);
    }
    @GetMapping("/favorites")
    public List<GameDto> getUserFavorites(Authentication authentication) {
        return interactionService.getUserFavorites(authentication);
    }
    @GetMapping("/favorites/filter")
    public Page<GameDto> getFilteredFavorites(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "0") int page) {
        
        return interactionService.getFavoritesFiltered(authentication, search, tagId, PageRequest.of(page, 10)).map(this::toGamesDto);
    }
    @GetMapping("/reviews/my")
    public List<ReviewDto> getMyReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        return interactionService.getUserReviews(authentication, page, size);
    }
    @PostMapping("/reviews/{reviewId}/react")
    public ResponseEntity<?> reactToReview(
            @PathVariable Long reviewId,
            @RequestParam String typeString,
            Authentication authentication) {
        
        interactionService.reactToReview(authentication, reviewId, typeString);
        
        return ResponseEntity.ok().build();
    }
    
}
