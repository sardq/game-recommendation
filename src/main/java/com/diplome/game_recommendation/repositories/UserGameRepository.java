package com.diplome.game_recommendation.repositories;

import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.InteractionEnum;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserGameRepository extends JpaRepository<UserGames, Long> {

    List<UserGames> findByUserId(Long userId);

    List<UserGames> findByGameId(Long gameId);
    int countByUserId(Long userId);
    List<UserGames> findByUserIdAndGameId(Long userId, Long gameId);
    List<UserGames> findByUserIdOrderByTimeDesc(Long userId);
    Page<UserGames> findByUserIdAndInteraction(Long userId, InteractionEnum interaction, Pageable pageable);
    List<UserGames> findByUserIdAndInteraction(Long userId, InteractionEnum interaction);
    Page<UserGames> findByGameIdAndReviewIsNotNullOrderByTimeDesc(Long gameId, Pageable pageable);
    Page<UserGames> findByGameIdAndRatingIsNotNullOrderByTimeDesc(Long gameId, Pageable pageable);
    Optional<UserGames> findByUserIdAndGameIdAndInteraction(Long userId,Long gameid, InteractionEnum interaction);
    @Query("SELECT AVG(ug.rating) FROM UserGames ug WHERE ug.game.id = :gameId AND ug.interaction = 'Rated'")
    Double getAverageRatingForGame(@Param("gameId") Long gameId);

    @Query("SELECT COUNT(ug) FROM UserGames ug WHERE ug.game.id = :gameId AND ug.interaction = 'Rated'")
    Integer getCountOfRatingsForGame(@Param("gameId") Long gameId);
    @Query("""
      SELECT DISTINCT ug.game FROM UserGames ug 
      LEFT JOIN ug.game.gameTags gt 
      WHERE ug.user.id = :userId 
        AND ug.interaction = 'Favorite'
        AND (
            CAST(:search AS string) IS NULL 
            OR LOWER(ug.game.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
        )
        AND (:tagId IS NULL OR gt.tag.id = :tagId)
      """)
  Page<GameEntity> findFavoritesFiltered(
      @Param("userId") Long userId, 
      @Param("search") String search, 
      @Param("tagId") Long tagId, 
      Pageable pageable);
}