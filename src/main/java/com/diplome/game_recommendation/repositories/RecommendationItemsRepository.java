package com.diplome.game_recommendation.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.diplome.game_recommendation.models.RecommendationItems;

public interface RecommendationItemsRepository 
        extends JpaRepository<RecommendationItems, Long> {

    List<RecommendationItems> findBySessionIdOrderByRank(Long sessionId);

}
