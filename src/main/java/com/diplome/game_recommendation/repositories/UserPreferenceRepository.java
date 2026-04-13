package com.diplome.game_recommendation.repositories;

import com.diplome.game_recommendation.models.UserPreference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    List<UserPreference> findByUserId(Long userId);
    void deleteByUserId(Long userId);
    List<UserPreference> findByUserIdOrderByPreferenceWeightDesc(Long userId);
    Optional<UserPreference> findByUserIdAndTagId(Long userId, Long tagId);
    boolean existsByUserId(Long userId);
    Long countByUserId(Long userId);
}