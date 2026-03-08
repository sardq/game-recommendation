package com.diplome.game_recommendation.repositories;

import com.diplome.game_recommendation.models.GameTag;
import com.diplome.game_recommendation.models.GameTagId;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.TagEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameTagRepository extends JpaRepository<GameTag, GameTagId> {

    List<GameTag> findByGame(GameEntity game);

    List<GameTag> findByTag(TagEntity tag);

}