package com.diplome.game_recommendation.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.diplome.game_recommendation.models.GameEntity;
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
    List<TagEntity> findByKeep(Boolean keep);
    Page<TagEntity> findByKeep(Boolean keep, Pageable pageable);
    @Query("""
    SELECT t
    FROM TagEntity t
    WHERE t.keep = true
      AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(t.nameRu) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<TagEntity> filterBySearch(String search, Pageable pageable);
    @Query("""
      SELECT t FROM TagEntity t JOIN UserPreference up ON t.id = up.tag.id 
     WHERE up.user.id = :userId ORDER BY up.preferenceWeight DESC
    """)
     Page<TagEntity> getTagsSortedByPreference(Long userId, Pageable pageable);
}
