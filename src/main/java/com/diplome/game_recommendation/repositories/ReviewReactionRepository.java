package com.diplome.game_recommendation.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.diplome.game_recommendation.models.ReactionType;
import com.diplome.game_recommendation.models.ReviewReaction;

public interface ReviewReactionRepository extends JpaRepository<ReviewReaction, Long> {
    long countByReviewIdAndType(Long reviewId, ReactionType type);
    Optional<ReviewReaction> findByUserIdAndReviewId(Long userId, Long reviewId);
    List<ReviewReaction> findByReviewId(Long reviewId);
    
    @Query("SELECT r.type, COUNT(r) FROM ReviewReaction r WHERE r.review.id = :reviewId GROUP BY r.type")
    List<Object[]> getReactionCounts(@Param("reviewId") Long reviewId);
}