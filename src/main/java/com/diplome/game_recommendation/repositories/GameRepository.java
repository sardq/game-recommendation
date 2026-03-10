package com.diplome.game_recommendation.repositories;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.diplome.game_recommendation.models.GameEntity;

public interface GameRepository extends JpaRepository<GameEntity, Long> {
    List<GameEntity> findByNameContainingIgnoreCase(String name);
    Page<GameEntity> findByRatingGreaterThanEqual(BigDecimal rating, Pageable pageable);
    List<GameEntity> findByRatingGreaterThanEqual(Double rating);
    Page<GameEntity> findByRatingGreaterThanEqual(Double rating, Pageable pageable);

    List<GameEntity> findByMetacriticRateGreaterThanEqual(Integer score);
    Page<GameEntity> findAll(Pageable pageable);
    //jpql запрос
    Page<GameEntity> findByTagId(Long tagId, Pageable pageable);
    Page<GameEntity> filterBySearch(String search, Pageable pageable);
    Page<GameEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
