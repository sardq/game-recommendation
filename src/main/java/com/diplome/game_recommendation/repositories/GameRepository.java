package com.diplome.game_recommendation.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.diplome.game_recommendation.models.GameEntity;

public interface GameRepository extends JpaRepository<GameEntity, Long> {

    Page<GameEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
    boolean existsByRawgId(Long rawgId);
    Page<GameEntity> findAll(Pageable pageable);

    Page<GameEntity> findByRatingGreaterThanEqual(Double rating, Pageable pageable);

    Page<GameEntity> findByMetacriticRateGreaterThanEqual(Double score, Pageable pageable);

    @Query("""
    SELECT g 
    FROM GameEntity g 
    JOIN GameTag gt ON g.id = gt.game.id
    WHERE gt.tag.id = :tagId
    """)
    Page<GameEntity> findByTagId(Long tagId, Pageable pageable);
    @Query("""
    SELECT DISTINCT g
    FROM GameEntity g
    JOIN GameTag gt ON g.id = gt.game.id
    WHERE gt.tag.id IN :tagIds
    """)
    Page<GameEntity> findByTagIds(List<Long> tagIds, Pageable pageable);
    @Query("""
    SELECT g
    FROM GameEntity g
    WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    Page<GameEntity> filterBySearch(String search, Pageable pageable);
    Page<GameEntity> findTop20ByOrderByReleaseDateDesc(Pageable pageable);
    Page<GameEntity> findTop20ByOrderByRatingDesc(Pageable pageable);
}
