package com.diplome.game_recommendation.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.diplome.game_recommendation.dtos.InteractionDto;
import com.diplome.game_recommendation.dtos.ReviewDto;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.InteractionEnum;
import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.repositories.GameRepository;
import com.diplome.game_recommendation.repositories.UserGameRepository;
import com.diplome.game_recommendation.repositories.UserRepository;

@Service
public class InteractionService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final UserGameRepository userGamesRepository;
    public InteractionService(
            UserRepository userRepository,
            GameRepository gameRepository,
            UserGameRepository userGamesRepository    ) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.userGamesRepository = userGamesRepository;
    }
    public void recordView(Authentication authentication, Long gameId) {

        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();

        UserGames interaction = new UserGames();

        interaction.setUser(user);
        interaction.setGame(game);
        interaction.setInteraction(InteractionEnum.Viewed);
        interaction.setTime(LocalDateTime.now());

        userGamesRepository.save(interaction);
    }

    public void recordRating(Authentication authentication, Long gameId, Integer rating) {

        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();

        UserGames interaction = userGamesRepository.findByUserIdAndGameIdAndInteraction(user.getId(), gameId, InteractionEnum.Rated).orElse(new UserGames());

        interaction.setUser(user);
        interaction.setGame(game);
        interaction.setInteraction(InteractionEnum.Rated);
        interaction.setRating(rating);
        interaction.setTime(LocalDateTime.now());

        userGamesRepository.save(interaction);
    }

    public void addToFavorites(Authentication authentication, Long gameId) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();
        UserGames interaction = new UserGames();
        interaction.setUser(user);
        interaction.setGame(game);
        interaction.setInteraction(InteractionEnum.Favorite);
        interaction.setTime(LocalDateTime.now());
        userGamesRepository.save(interaction);
    }
    public void removeFromFavorites(Authentication authentication, Long gameId) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();
        var interaction = userGamesRepository
                .findByUserIdAndGameIdAndInteraction(user.getId(), game.getId(), InteractionEnum.Favorite);

        interaction.ifPresent(userGamesRepository::delete);
    }
    public boolean isFavorite(Authentication authentication, Long gameId){

        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();

        return userGamesRepository
                .findByUserIdAndGameIdAndInteraction(user.getId(), game.getId(), InteractionEnum.Favorite)
                .isPresent();
    }
    public List<ReviewDto> getReviewsByGame(Long gameId) {
        return userGamesRepository
                .findByGameIdAndReviewIsNotNullOrderByTimeDesc(gameId).stream().map(this::toReviewDto).toList();
    }
    public void addReview(Authentication authentication, Long gameId, String reviewText) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();

        UserGames ug = userGamesRepository
            .findByUserIdAndGameIdAndInteraction(user.getId(), gameId, InteractionEnum.Review)
            .orElse(new UserGames(user, game, InteractionEnum.Review, null, LocalDateTime.now(), reviewText));

        ug.setReview(reviewText);
        ug.setTime(LocalDateTime.now());

        userGamesRepository.save(ug);
    }
    public Optional<UserGames> getUserInteraction(Authentication authentication, Long gameId, String type) {
    Optional<UserEntity> user = userRepository.findByEmail(authentication.getName());
    return userGamesRepository.findByUserIdAndGameIdAndInteraction(user.get().getId(), gameId, InteractionEnum.valueOf(type));
}
private ReviewDto toReviewDto(UserGames ug) {
    ReviewDto dto = new ReviewDto();
    dto.setId(ug.getId());
    dto.setReview(ug.getReview());          
    dto.setLogin(ug.getUser().getUsername()); 
    return dto;
}
}