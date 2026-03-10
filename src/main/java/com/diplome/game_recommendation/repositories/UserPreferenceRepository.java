package com.diplome.game_recommendation.repositories;

import com.diplome.game_recommendation.models.UserPreference;
import com.diplome.game_recommendation.models.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    List<UserPreference> findByUserId(Long userId);
    void deleteByUserId(Long userId);

}