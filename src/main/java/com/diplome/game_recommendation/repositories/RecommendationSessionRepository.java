package com.diplome.game_recommendation.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.diplome.game_recommendation.models.RecommendationSession;

public interface RecommendationSessionRepository 
        extends JpaRepository<RecommendationSession, Long> {
    
    List<RecommendationSession> findByUserIdOrderByGeneratedAtDesc(Long userId);

}
