package com.diplome.game_recommendation.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.diplome.game_recommendation.dtos.RecommendationDto;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.GameTag;
import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.models.UserPreference;
import com.diplome.game_recommendation.repositories.GameRepository;
import com.diplome.game_recommendation.repositories.GameTagRepository;
import com.diplome.game_recommendation.repositories.UserPreferenceRepository;

@Service
public class RecomendationService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final GameRepository gameRepository;
    private final GameTagRepository gameTagRepository;

    public RecomendationService(
            UserPreferenceRepository userPreferenceRepository,
            GameRepository gameRepository,
            GameTagRepository gameTagRepository
    ) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.gameRepository = gameRepository;
        this.gameTagRepository = gameTagRepository;
    }

    public List<RecommendationDto> getRecommendationsForUser(Long userId) {
        List<RecommendationDto> content = getContentBasedRecommendations(userId);

        if (content.isEmpty()) {
            return getCollaborativeRecommendations(userId);
        }

        return content;
    }
    public List<RecommendationDto> getContentBasedRecommendations(Long userId) {

    List<UserPreference> preferences =
            userPreferenceRepository.findByUserId(userId);

    List<GameEntity> games = gameRepository.findAll();

    Map<Long, Double> scores = new HashMap<>();

    for (GameEntity game : games) {

        List<GameTag> tags = gameTagRepository.findByGame(game);

        double score = 0;

        for (GameTag gameTag : tags) {

            for (UserPreference pref : preferences) {

                if (pref.getTag().getId()
                        .equals(gameTag.getTag().getId())) {

                    score += pref.getPreferenceWeight().doubleValue();
                }
            }
        }

        scores.put(game.getId(), score);
    }

    return scores.entrySet()
            .stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(20)
            .map(e -> mapToRecommendation(e.getKey(), e.getValue()))
            .toList();
}
public List<RecommendationDto> getSimilarGames(Long gameId) {

    GameEntity game = gameRepository.findById(gameId).orElseThrow();

    List<GameTag> tags = gameTagRepository.findByGame(game);

    Set<TagEntity> tagSet = tags.stream()
            .map(GameTag::getTag)
            .collect(Collectors.toSet());

    List<GameEntity> games = gameRepository.findAll();

    Map<Long, Integer> similarity = new HashMap<>();

    for (GameEntity g : games) {

        List<GameTag> gameTags = gameTagRepository.findByGame(g);

        int common = 0;

        for (GameTag gt : gameTags) {
            if (tagSet.contains(gt.getTag())) {
                common++;
            }
        }

        similarity.put(g.getId(), common);
    }

    return similarity.entrySet()
            .stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(20)
            .map(e -> mapToRecommendation(e.getKey(), e.getValue()))
            .toList();
}
public List<RecommendationDto> getCollaborativeRecommendations(Long userId) {

    return List.of();

}
private RecommendationDto mapToRecommendation(Long gameId, Number score) {

    GameEntity game = gameRepository.findById(gameId).orElseThrow();

    RecommendationDto dto = new RecommendationDto();

    dto.setGameId(game.getId());
    dto.setName(game.getName());
    dto.setPosterUrl(game.getPosterUrl());
    dto.setRating(game.getRating());
    dto.setRecommendationScore(score.doubleValue());

    return dto;
}
}