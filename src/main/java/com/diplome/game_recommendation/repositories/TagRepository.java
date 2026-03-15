package com.diplome.game_recommendation.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.diplome.game_recommendation.models.TagEntity;

public interface TagRepository extends JpaRepository<TagEntity, Long> {

    Optional<TagEntity> findByName(String name);
    @Query("""
    SELECT t
    FROM TagEntity t
    JOIN GameTag gt ON t.id = gt.tag.id
    GROUP BY t
    ORDER BY COUNT(gt.game.id) DESC
    """)
    List<TagEntity> findPopularTags(Pageable pageable);
    @Query("""
    SELECT t
    FROM TagEntity t
    JOIN GameTag gt ON t.id = gt.tag.id
    WHERE gt.game.id = :gameId
    """)
    List<TagEntity> findTagsByGameId(Long gameId);
    
}
