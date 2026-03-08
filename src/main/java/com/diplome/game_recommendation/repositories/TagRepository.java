package com.diplome.game_recommendation.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.diplome.game_recommendation.models.TagEntity;

public interface TagRepository extends JpaRepository<TagEntity, Long> {

    Optional<TagEntity> findByName(String name);

}
