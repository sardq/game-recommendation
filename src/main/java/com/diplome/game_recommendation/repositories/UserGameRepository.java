package com.diplome.game_recommendation.repositories;

import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.InteractionEnum;
import com.diplome.game_recommendation.models.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGameRepository extends JpaRepository<UserGames, Long> {

    List<UserGames> findByUserId(UserEntity userId);

    List<UserGames> findByGameId(GameEntity gameId);

    List<UserGames> findByUserIdAndGameId(UserEntity userId, GameEntity gameId);
    List<UserGames> findByUserIdOrderByTimeDesc(UserEntity userId);
    List<UserGames> findByUserIdAndInteraction(UserEntity userId, InteractionEnum interaction);
}