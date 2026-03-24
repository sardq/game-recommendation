package com.diplome.game_recommendation.repositories;

import com.diplome.game_recommendation.models.GameTag;
import com.diplome.game_recommendation.models.GameTagId;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameTagRepository extends JpaRepository<GameTag, GameTagId> {

    List<GameTag> findByGameId(Long gameId);

    List<GameTag> findByTagId(Long tagId);
    boolean existsByGameIdAndTagId(Long gameId, Long TagId);

}