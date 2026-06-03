package com.diplome.game_recommendation.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.diplome.game_recommendation.dtos.TagPreferenceDto;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.GameTag;
import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.models.UserPreference;
import com.diplome.game_recommendation.repositories.GameTagRepository;
import com.diplome.game_recommendation.repositories.TagRepository;
import com.diplome.game_recommendation.repositories.UserGameRepository;
import com.diplome.game_recommendation.repositories.UserPreferenceRepository;
import com.diplome.game_recommendation.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserPreferenceService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserGameRepository userGamesRepository;
    private final GameTagRepository gameTagRepository;
    private final TagRepository tagRepository;
    UserPreferenceService(UserRepository userRepository, UserPreferenceRepository userPreferenceRepository, UserGameRepository userGameRepository, GameTagRepository gameTagRepository,
        TagRepository tagRepository) {
            this.gameTagRepository = gameTagRepository;
            this.userPreferenceRepository = userPreferenceRepository;
            this.userGamesRepository = userGameRepository;
            this.userRepository = userRepository;
            this.tagRepository = tagRepository;
        }
    public List<UserPreference> getUserPreferences(Authentication authentication) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElse(null);
        return userPreferenceRepository.findByUserId(user.getId());
    }
    public void updateUserPreferences(Long userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        List<UserGames> interactions = userGamesRepository.findByUserId(user.getId());
        Map<Long, Double> tagWeights = new HashMap<>();
        for (UserGames interaction : interactions) {
            GameEntity game = interaction.getGame();
            List<GameTag> tags = gameTagRepository.findByGameId(game.getId());
            double weight = getInteractionWeight(interaction);
            for (GameTag gameTag : tags) {
                Long tagId = gameTag.getTag().getId();
                tagWeights.put(
                        tagId,
                        tagWeights.getOrDefault(tagId, 0.0) + weight
                );
            }
        }
        savePreferences(user, tagWeights);
    }
    private double getInteractionWeight(UserGames interaction) {
        switch (interaction.getInteraction()) {
            case Viewed:
                return 0.2;
            case Favorite:
                return 0.8;
            case Rated:
                return interaction.getRating() / 5.0;
            default:
                return 0;
        }
    }
    private void savePreferences(UserEntity user, Map<Long, Double> tagWeights) {
        userPreferenceRepository.deleteByUserId(user.getId());
        for (Map.Entry<Long, Double> entry : tagWeights.entrySet()) {
            TagEntity tag = tagRepository.findById(entry.getKey()).orElseThrow();
            UserPreference preference = new UserPreference();
            preference.setUser(user);
            preference.setTag(tag);
            preference.setPreferenceWeight(
                    Double.valueOf(entry.getValue())
            );
            userPreferenceRepository.save(preference);
        }
    }
    public void initializeColdStartPreferences(Long userId, List<Long> tagIds) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        for (Long tagId : tagIds) {
            TagEntity tag = tagRepository.findById(tagId).orElseThrow();
            UserPreference preference = new UserPreference();
            preference.setUser(user);
            preference.setTag(tag);
            preference.setPreferenceWeight(Double.valueOf(1.0));
            userPreferenceRepository.save(preference);
        }
    }
    @Transactional
    public void initializeColdStartPreferencesWithRating(
        Authentication authentication,
        List<TagPreferenceDto> tags
    ) {
        String name = authentication.getName();
        UserEntity user = userRepository.findByEmail(name).orElseThrow();

        userPreferenceRepository.deleteByUserId(user.getId());

        for (TagPreferenceDto dto : tags) {
            TagEntity tag = tagRepository.findById(dto.tagId).orElseThrow();

            double weight = dto.rating / 5.0; 

            UserPreference pref = new UserPreference();
            pref.setUser(user);
            pref.setTag(tag);
            pref.setPreferenceWeight(Double.valueOf(weight));

            userPreferenceRepository.save(pref);
        }
    }
    @Transactional
public Long countByUserId( Authentication authentication) {
    UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
    return userPreferenceRepository.countByUserId(user.getId());
    }
}
