package com.diplome.game_recommendation.repositories;

import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.models.InteractionEnum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserGameRepository extends JpaRepository<UserGames, Long> {

    List<UserGames> findByUserId(Long userId);

    List<UserGames> findByGameId(Long gameId);
    int countByUserId(Long userId);
    List<UserGames> findByUserIdAndGameId(Long userId, Long gameId);
    List<UserGames> findByUserIdOrderByTimeDesc(Long userId);
    List<UserGames> findByUserIdAndInteraction(Long userId, InteractionEnum interaction);
    Optional<UserGames> findByUserIdAndGameIdAndInteraction(Long userId,Long gameid, InteractionEnum interaction);
}