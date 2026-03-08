package com.diplome.game_recommendation.repositories;

import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGameRepository extends JpaRepository<UserGames, Long> {

    List<UserGames> findByUser(UserEntity user);

    List<UserGames> findByGame(GameEntity game);

    List<UserGames> findByUserAndGame(UserEntity user, GameEntity game);

}