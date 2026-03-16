package com.diplome.game_recommendation.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.diplome.game_recommendation.dtos.RecommendationDto;
import com.diplome.game_recommendation.dtos.RecommendationSessionDetailsDto;
import com.diplome.game_recommendation.dtos.RecommendationSessionDto;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.GameTag;
import com.diplome.game_recommendation.models.RecommendationItems;
import com.diplome.game_recommendation.models.RecommendationSession;
import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.models.UserPreference;
import com.diplome.game_recommendation.repositories.GameRepository;
import com.diplome.game_recommendation.repositories.GameTagRepository;
import com.diplome.game_recommendation.repositories.RecommendationItemsRepository;
import com.diplome.game_recommendation.repositories.RecommendationSessionRepository;
import com.diplome.game_recommendation.repositories.UserGameRepository;
import com.diplome.game_recommendation.repositories.UserPreferenceRepository;
import com.diplome.game_recommendation.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class RecomendationService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final GameRepository gameRepository;
    private final UserGameRepository userGameRepository;
    private final UserRepository userRepository;
    private final RecommendationSessionRepository sessionRepository;
    private final RecommendationItemsRepository recommendationItemsRepository;
    private final GameTagRepository gameTagRepository;

    public RecomendationService(
            UserPreferenceRepository userPreferenceRepository,
            GameRepository gameRepository,
            RecommendationSessionRepository sessionRepository,
            UserGameRepository userGameRepository,
            UserRepository userRepository,
            RecommendationItemsRepository recommendationItemsRepository,
            GameTagRepository gameTagRepository
    ) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.gameRepository = gameRepository;
        this.sessionRepository = sessionRepository;
        this.userGameRepository = userGameRepository;
        this.userRepository = userRepository;
        this.recommendationItemsRepository = recommendationItemsRepository;
        this.gameTagRepository = gameTagRepository;
    }

    public List<RecommendationDto> getRecommendationsForUser(Long userId) {
        int interactions = userGameRepository.countByUserId(userId);

    if(interactions < 3){
        return getColdStartRecommendations();
    }

    List<RecommendationDto> collaborative =
            getCollaborativeRecommendations(userId);

    if(!collaborative.isEmpty()){
        return collaborative;
    }

    return getContentBasedRecommendations(userId);
}
    public List<RecommendationDto> getContentBasedRecommendations(Long userId){

        List<UserPreference> prefs =
                userPreferenceRepository.findByUserId(userId);

        Map<Long,Double> userVector =
                prefs.stream()
                        .collect(Collectors.toMap(
                                p -> p.getTag().getId(),
                                p -> p.getPreferenceWeight().doubleValue()
                        ));

        List<GameEntity> games = gameRepository.findAll();

        Map<Long,Double> scores = new HashMap<>();

        for(GameEntity game : games){

            List<GameTag> tags =
                    gameTagRepository.findByGameId(game.getId());

            Map<Long,Double> gameVector = new HashMap<>();

            for(GameTag tag : tags){
                gameVector.put(tag.getTag().getId(),1.0);
            }

            double score = cosineSimilarity(userVector,gameVector);

            scores.put(game.getId(),score);
        }

        return scores.entrySet()
                .stream()
                .sorted(Map.Entry.<Long,Double>comparingByValue().reversed())
                .limit(20)
                .map(e -> mapToRecommendation(e.getKey(),e.getValue()))
                .toList();
    }
    private double pearsonCorrelation(
        Map<Long, Double> userRatings,
        Map<Long, Double> otherRatings
    ){

        Set<Long> commonGames =
                userRatings.keySet()
                        .stream()
                        .filter(otherRatings::containsKey)
                        .collect(Collectors.toSet());
        int n = commonGames.size();
        if(n == 0){
            return 0;
        }
        double sum1 = 0;
        double sum2 = 0;
        double sum1Sq = 0;
        double sum2Sq = 0;
        double pSum = 0;
        for(Long gameId : commonGames){
            double r1 = userRatings.get(gameId);
            double r2 = otherRatings.get(gameId);
            sum1 += r1;
            sum2 += r2;
            sum1Sq += Math.pow(r1,2);
            sum2Sq += Math.pow(r2,2);
            pSum += r1*r2;
        }
        double num = pSum - (sum1*sum2/n);
        double den = Math.sqrt(
                (sum1Sq - Math.pow(sum1,2)/n) *
                (sum2Sq - Math.pow(sum2,2)/n)
        );
        if(den == 0){
            return 0;
        }
        return num/den;
    }
    public List<RecommendationDto> getCollaborativeRecommendations(Long userId){
        List<UserGames> userGames =
                userGameRepository.findByUserId(userId);

        Map<Long, Double> userRatings = userGames.stream()
                .collect(Collectors.toMap(
                        g -> g.getGame().getId(),
                        g -> g.getRating().doubleValue()
                ));

        List<UserEntity> users = userRepository.findAll();
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Double> similaritySum = new HashMap<>();
        for(UserEntity other : users){
            if(other.getId().equals(userId)){
                continue;
            }
            List<UserGames> otherGames =
                    userGameRepository.findByUserId(other.getId());
            Map<Long, Double> otherRatings =
                    otherGames.stream()
                            .collect(Collectors.toMap(
                                    g -> g.getGame().getId(),
                                    g -> g.getRating().doubleValue()
                            ));
            double similarity =
                    pearsonCorrelation(userRatings, otherRatings);
            if(similarity <= 0){
                continue;
            }
            for(UserGames g : otherGames){
                Long gameId = g.getGame().getId();
                if(userRatings.containsKey(gameId)){
                    continue;
                }
                scores.put(
                        gameId,
                        scores.getOrDefault(gameId,0.0)
                                + similarity * g.getRating().doubleValue()
                );
                similaritySum.put(
                        gameId,
                        similaritySum.getOrDefault(gameId,0.0)
                                + similarity
                );
            }
        }
        Map<Long, Double> rankings = new HashMap<>();
        for(Long gameId : scores.keySet()){
            rankings.put(
                    gameId,
                    scores.get(gameId) / similaritySum.get(gameId)
            );
        }
        return rankings.entrySet()
                .stream()
                .sorted(Map.Entry.<Long,Double>comparingByValue().reversed())
                .limit(20)
                .map(e -> mapToRecommendation(e.getKey(), e.getValue()))
                .toList();
    }
    public List<RecommendationSessionDto> getUserSessions(Long userId){
        return sessionRepository
                .findByUserIdOrderByGeneratedAtDesc(userId)
                .stream()
                .map(s -> {
                    RecommendationSessionDto dto = new RecommendationSessionDto();
                    dto.setId(s.getId());
                    dto.setGeneratedAt(s.getGeneratedAt());
                    dto.setItemsCount(s.getItems().size());
                    return dto;
                })
                .toList();
    }
    public RecommendationSessionDetailsDto getSession(Long sessionId){
        RecommendationSession session =
                sessionRepository.findById(sessionId).orElseThrow();
        List<RecommendationItems> items =
                recommendationItemsRepository
                        .findBySessionIdOrderByRank(sessionId);
        RecommendationSessionDetailsDto dto =
                new RecommendationSessionDetailsDto();
        dto.setId(session.getId());
        dto.setGeneratedAt(session.getGeneratedAt());
        dto.setItems(
                items.stream()
                        .map(i -> mapToRecommendation(
                                i.getGame().getId(),
                                i.getScore()))
                        .toList()
        );
        return dto;
    }
    private double cosineSimilarity(
        Map<Long, Double> userVector,
        Map<Long, Double> gameVector){

        double dotProduct = 0;
        double userNorm = 0;
        double gameNorm = 0;

        for(Long tagId : gameVector.keySet()){

            double u = userVector.getOrDefault(tagId,0.0);
            double g = gameVector.get(tagId);

            dotProduct += u*g;

            userNorm += u*u;
            gameNorm += g*g;
        }

        if(userNorm == 0 || gameNorm == 0){
            return 0;
        }

        return dotProduct / (Math.sqrt(userNorm)*Math.sqrt(gameNorm));
    }
    public List<RecommendationDto> getSimilarGames(Long gameId) {
        GameEntity game = gameRepository.findById(gameId).orElseThrow();
        List<GameTag> tags = gameTagRepository.findByGameId(game.getId());
        Set<TagEntity> tagSet = tags.stream()
                .map(GameTag::getTag)
                .collect(Collectors.toSet());
        List<GameEntity> games = gameRepository.findAll();
        Map<Long, Integer> similarity = new HashMap<>();
        for (GameEntity g : games) {
            List<GameTag> gameTags = gameTagRepository.findByGameId(g.getId());
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
    public List<RecommendationDto> getColdStartRecommendations(){
        Page<GameEntity> games =
                gameRepository.findByRatingGreaterThanEqual(8.0, PageRequest.of(0,20));
        return games.stream()
                .limit(20)
                .map(g -> mapToRecommendation(g.getId(), g.getRating()))
                .toList();
    }
    @Transactional
    public void generateRecommendationSession(Long userId){

        List<RecommendationDto> recommendations =
                getRecommendationsForUser(userId);

        UserEntity user = userRepository.findById(userId).orElseThrow();

        RecommendationSession session = new RecommendationSession(user, LocalDateTime.now());

        sessionRepository.save(session);

        int rank = 1;

        for(RecommendationDto dto : recommendations){

            GameEntity game = gameRepository
                    .findById(dto.getGameId())
                    .orElseThrow();

            RecommendationItems item = new RecommendationItems(game, session, rank, dto.getRecommendationScore());

            recommendationItemsRepository.save(item);

        }

    }
}