package com.diplome.game_recommendation.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

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
            UserGameRepository userGamesRepository
    ) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.userGamesRepository = userGamesRepository;
    }
    public void recordView(Long userId, Long gameId) {

        UserEntity user = userRepository.findById(userId).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();

        UserGames interaction = new UserGames();

        interaction.setUser(user);
        interaction.setGame(game);
        interaction.setInteraction(InteractionEnum.Viewed);
        interaction.setTime(LocalDateTime.now());

        userGamesRepository.save(interaction);
    }

    public void recordRating(Long userId, Long gameId, Integer rating) {

        UserEntity user = userRepository.findById(userId).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();

        UserGames interaction = new UserGames();

        interaction.setUser(user);
        interaction.setGame(game);
        interaction.setInteraction(InteractionEnum.Rated);
        interaction.setRating(rating);
        interaction.setTime(LocalDateTime.now());

        userGamesRepository.save(interaction);
    }

    public void addToFavorites(Long userId, Long gameId) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();
        UserGames interaction = new UserGames();
        interaction.setUser(user);
        interaction.setGame(game);
        interaction.setInteraction(InteractionEnum.Favorite);
        interaction.setTime(LocalDateTime.now());
        userGamesRepository.save(interaction);
    }
    public void removeFromFavorites(Long userId, Long gameId) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();
        var interaction = userGamesRepository
                .findByUserIdAndGameIdAndInteraction(user.getId(), game.getId(), InteractionEnum.Favorite);

        interaction.ifPresent(userGamesRepository::delete);
    }
    public boolean isFavorite(Long userId, Long gameId){

    UserEntity user = userRepository.findById(userId).orElseThrow();
    GameEntity game = gameRepository.findById(gameId).orElseThrow();

    return userGamesRepository
            .findByUserIdAndGameIdAndInteraction(user.getId(), game.getId(), InteractionEnum.Favorite)
            .isPresent();
    }
}