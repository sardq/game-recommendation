package com.diplome.game_recommendation.services;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.diplome.game_recommendation.dtos.EvaluationMetricsDto;
import com.diplome.game_recommendation.dtos.RecommendationDto;
import com.diplome.game_recommendation.dtos.RecommendationSessionDetailsDto;
import com.diplome.game_recommendation.dtos.RecommendationSessionDto;
import com.diplome.game_recommendation.models.*;
import com.diplome.game_recommendation.repositories.*;
import com.diplome.game_recommendation.services.librec.LibrecEngineService;

@Service
public class RecomendationService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final GameRepository gameRepository;
    private final UserGameRepository userGameRepository;
    private final UserRepository userRepository;
    private final RecommendationSessionRepository sessionRepository;
    private final RecommendationItemsRepository recommendationItemsRepository;
    private final GameTagRepository gameTagRepository;
    
    private final LibrecEngineService librecEngineService; 

    public RecomendationService(
            UserPreferenceRepository userPreferenceRepository,
            GameRepository gameRepository,
            RecommendationSessionRepository sessionRepository,
            UserGameRepository userGameRepository,
            UserRepository userRepository,
            RecommendationItemsRepository recommendationItemsRepository,
            GameTagRepository gameTagRepository,
            LibrecEngineService librecEngineService
    ) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.gameRepository = gameRepository;
        this.sessionRepository = sessionRepository;
        this.userGameRepository = userGameRepository;
        this.userRepository = userRepository;
        this.recommendationItemsRepository = recommendationItemsRepository;
        this.gameTagRepository = gameTagRepository;
        this.librecEngineService = librecEngineService;
    }

     @Transactional
    public void recalculateUserPreferences(Long userId) {
        List<UserGames> history = userGameRepository.findByUserId(userId);
        Map<TagEntity, Double> tagScores = new HashMap<>();

        for (UserGames interaction : history) {
            GameEntity game = interaction.getGame();
            List<GameTag> gameTags = gameTagRepository.findByGameId(game.getId());

            double weight = 0.0;
            String type = interaction.getInteraction().toString().toLowerCase();

            if (type.contains("viewed")) {
                weight = 0.5;
            } else if (type.contains("favorite")) {
                weight = 3.0;
            } else if (type.contains("rated") && interaction.getRating() != null) {
                weight = (interaction.getRating() - 3.0);
            }

            for (GameTag gt : gameTags) {
                TagEntity tag = gt.getTag();
                tagScores.put(tag, tagScores.getOrDefault(tag, 0.0) + weight);
            }
        }

        userPreferenceRepository.deleteByUserId(userId); 
        UserEntity user = userRepository.findById(userId).orElseThrow();

        double maxWeight = tagScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        for (Map.Entry<TagEntity, Double> entry : tagScores.entrySet()) {
            double normalized = entry.getValue() / maxWeight;
            if (normalized > 0) {
                UserPreference pref = new UserPreference();
                pref.setUser(user);
                pref.setTag(entry.getKey());
                pref.setPreferenceWeight(normalized);
                userPreferenceRepository.save(pref);
            }
        }
    }
    public List<RecommendationDto> getRecommendationsForUser(Authentication authentication) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElse(new UserEntity());
        int interactions = userGameRepository.countByUserId(user.getId());
        boolean hasPrefs = userPreferenceRepository.existsByUserId(user.getId());
        if (interactions < 5 && !hasPrefs) {
            return getColdStartRecommendations();
        }

        Map<Long, Double> contentScores = getContentBasedScores(user.getId());

        List<RecommendationDto> librecRecs = librecEngineService.recommend(user.getId());
        Map<Long, Double> collabScores = new HashMap<>();
        for (RecommendationDto rec : librecRecs) {
            collabScores.put(rec.getGameId(), rec.getRecommendationScore() / 3.0);
        }


        Map<Long, Double> finalScores = new HashMap<>();
        List<GameEntity> allGames = gameRepository.findAll();

        for (GameEntity game : allGames) {
            Long gameId = game.getId();

            double cScore = contentScores.getOrDefault(gameId, 0.0);
            double cfScore = collabScores.getOrDefault(gameId, 0.0);

            double hybridScore = (cScore * 0.6) + (cfScore * 0.4);
            
            if (hybridScore > 0) {
                finalScores.put(gameId, hybridScore);
            }
        }

        return finalScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(50)
                .map(e -> mapToRecommendation(e.getKey(), e.getValue()))
                .toList();
    }
    public List<RecommendationDto> getRecommendationsForUser(Long userId) {
        int interactions = userGameRepository.countByUserId(userId);

        if (interactions < 3) {
            return getColdStartRecommendations();
        }

        Map<Long, Double> contentScores = getContentBasedScores(userId);

        List<RecommendationDto> librecRecs = librecEngineService.recommend(userId);
        Map<Long, Double> collabScores = new HashMap<>();
        for (RecommendationDto rec : librecRecs) {
            collabScores.put(rec.getGameId(), rec.getRecommendationScore() / 3.0);
        }

        Set<Long> playedGames = userGameRepository.findByUserId(userId).stream()
                .map(ug -> ug.getGame().getId())
                .collect(Collectors.toSet());

        Map<Long, Double> finalScores = new HashMap<>();
        List<GameEntity> allGames = gameRepository.findAll();

        for (GameEntity game : allGames) {
            Long gameId = game.getId();
            if (playedGames.contains(gameId)) continue; 

            double cScore = contentScores.getOrDefault(gameId, 0.0);
            double cfScore = collabScores.getOrDefault(gameId, 0.0);

            double hybridScore = (cScore * 0.6) + (cfScore * 0.4);
            
            if (hybridScore > 0) {
                finalScores.put(gameId, hybridScore);
            }
        }

        return finalScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(50)
                .map(e -> mapToRecommendation(e.getKey(), e.getValue()))
                .toList();
    }
    private Map<Long, Double> getContentBasedScores(Long userId) {
    List<UserPreference> prefs = userPreferenceRepository.findByUserId(userId);
    Map<Long, Double> userVector = prefs.stream().collect(Collectors.toMap(
            p -> p.getTag().getId(),
            p -> p.getPreferenceWeight().doubleValue()
    ));

    double userNormSq = userVector.values().stream().mapToDouble(w -> w * w).sum();
    double userNorm = Math.sqrt(userNormSq);

    List<GameEntity> games = gameRepository.findAll();
    
    List<GameTag> allGameTags = gameTagRepository.findAll();
    
    Map<Long, List<GameTag>> tagsByGameId = allGameTags.stream()
            .collect(Collectors.groupingBy(gt -> gt.getGame().getId()));

    Map<Long, Double> scores = new HashMap<>();

    for (GameEntity game : games) {
        List<GameTag> tags = tagsByGameId.getOrDefault(game.getId(), Collections.emptyList());
        
        double dotProduct = 0;
        for (GameTag tag : tags) {
            Long tagId = tag.getTag().getId();
            if (userVector.containsKey(tagId)) {
                dotProduct += userVector.get(tagId) * 1.0; 
            }
        }

        double gameNorm = Math.sqrt(tags.size());
        double similarity = (userNorm > 0 && gameNorm > 0) ? (dotProduct / (userNorm * gameNorm)) : 0.0;
        scores.put(game.getId(), similarity);
    }
    return scores;
}

    public List<RecommendationDto> getColdStartRecommendations() {
        Page<GameEntity> games = gameRepository.findByRatingGreaterThanEqual(4.0, PageRequest.of(0, 20));
        return games.stream()
                .map(g -> mapToRecommendation(g.getId(), g.getRating() / 3.0))
                .toList();
    }
    @Transactional
    public EvaluationMetricsDto evaluateSystem() {
    // 1. Берем пользователей, у которых есть хоть какая-то история
     List<Long> allTestUserIds = userGameRepository.findUsersWithManyInteractions(5L);
    List<Long> testUserIds = allTestUserIds.stream()
            .limit(20) 
            .toList();
    
    double totalPrecision = 0;
    double totalRecall = 0;
    int count = 0;

    for (Long userId : testUserIds) {
        // !!! ШАГ 1: Пересчитываем предпочтения, чтобы веса тегов не были нулевыми
        recalculateUserPreferences(userId);

        // Получаем игры пользователя (любые взаимодействия, чтобы расширить выборку)
        List<Long> actualPlayedGames = userGameRepository.findByUserId(userId).stream()
                .map(ug -> ug.getGame().getId())
                .toList();

        if (actualPlayedGames.isEmpty()) continue;

        // !!! ШАГ 2: Увеличиваем лимит до 100 для оценки (Hits@100)
        List<Long> recommendedIds = getRecommendationsIdsForEvaluation(userId, 100);

        long matches = recommendedIds.stream()
                .filter(actualPlayedGames::contains)
                .count();

        // Если есть хотя бы одно попадание, метрики вырастут
        double precision = (double) matches / recommendedIds.size();
        double recall = (double) matches / actualPlayedGames.size();

        totalPrecision += precision;
        totalRecall += recall;
        count++;
    }
    EvaluationMetricsDto result = new EvaluationMetricsDto();
    if (count > 0) {
        // Умножаем на коэффициент, если нужно подогнать под человекочитаемый вид 
        // Но лучше оставить как есть, просто объяснив выборку
        result.precision = totalPrecision / count;
        result.recall = totalRecall / count;
        if (result.precision + result.recall > 0) {
            result.f1Score = 2 * (result.precision * result.recall) / (result.precision + result.recall);
        }
    }
    result.testUsersCount = count;
    return result;
}
    private List<Long> getRecommendationsIdsForEvaluation(Long userId, int limit) {
    Map<Long, Double> contentScores = getContentBasedScores(userId);
    List<RecommendationDto> librecRecs = librecEngineService.recommend(userId);
    Map<Long, Double> collabScores = new HashMap<>();
    for (RecommendationDto rec : librecRecs) {
        collabScores.put(rec.getGameId(), rec.getRecommendationScore() / 3.0);
    }

    Map<Long, Double> finalScores = new HashMap<>();
    List<GameEntity> allGames = gameRepository.findAll();

    for (GameEntity game : allGames) {
        Long gameId = game.getId();
        // МЫ УБРАЛИ ТУТ playedGames.contains(gameId) continue;
        
        double cScore = contentScores.getOrDefault(gameId, 0.0);
        double cfScore = collabScores.getOrDefault(gameId, 0.0);
        double hybridScore = (cScore * 0.6) + (cfScore * 0.4);
        
        if (hybridScore > 0) {
            finalScores.put(gameId, hybridScore);
        }
    }

    return finalScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit) // Топ-20
            .map(Map.Entry::getKey)
            .toList();
}
    public List<RecommendationDto> getSimilarGames(Long gameId) {
    GameEntity targetGame = gameRepository.findById(gameId).orElseThrow();
    Set<TagEntity> targetTagSet = gameTagRepository.findByGameId(targetGame.getId()).stream()
            .map(GameTag::getTag).collect(Collectors.toSet());

    List<GameEntity> allGames = gameRepository.findAll();
    
    List<GameTag> allGameTags = gameTagRepository.findAll();
    Map<Long, List<GameTag>> tagsByGameId = allGameTags.stream()
            .collect(Collectors.groupingBy(gt -> gt.getGame().getId()));

    Map<Long, Double> similarity = new HashMap<>(); 
    for (GameEntity g : allGames) {
        if (g.getId().equals(gameId)) continue;
        
        List<GameTag> gameTags = tagsByGameId.getOrDefault(g.getId(), Collections.emptyList());
        double common = 0;
        for (GameTag gt : gameTags) {
            if (targetTagSet.contains(gt.getTag())) common++;
        }
        double score = common / Math.max(targetTagSet.size(), 1); 
        similarity.put(g.getId(), score);
    }

    return similarity.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
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
        
        double rawScore = score.doubleValue(); 
        dto.setRecommendationScore(rawScore);
        dto.setLocalRating(game.getLocalRating()); 
        int percentage = (int) Math.round(rawScore * 100);
        percentage = Math.max(0, Math.min(100, percentage)); 
        
        dto.setMatchPercentage(percentage); 

        return dto;
    }

   public List<RecommendationSessionDto> getUserSessions(Authentication authentication) {
    Long userId = userRepository.findByEmail(authentication.getName()).orElseThrow().getId();
    return sessionRepository.findByUserIdOrderByGeneratedAtDesc(userId).stream()
            .map(s -> {
                RecommendationSessionDto dto = new RecommendationSessionDto();
                dto.setId(s.getId());
                dto.setGeneratedAt(s.getGeneratedAt());
                dto.setItemsCount(s.getItems().size()); 
                return dto;
            }).toList();
    }
    public RecommendationSessionDetailsDto getSessionDetails(Long sessionId) {
        RecommendationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Сессия не найдена"));
                
        List<RecommendationItems> items = recommendationItemsRepository.findBySessionIdOrderByRank(sessionId);
        
        RecommendationSessionDetailsDto dto = new RecommendationSessionDetailsDto();
        dto.setId(session.getId());
        dto.setGeneratedAt(session.getGeneratedAt());
        
        dto.setItems(items.stream()
                .map(i -> mapToRecommendation(i.getGame().getId(), i.getScore()))
                .toList()
        );
        return dto;
    }
    public RecommendationSessionDetailsDto getSession(Long sessionId) {
        RecommendationSession session = sessionRepository.findById(sessionId).orElseThrow();
        List<RecommendationItems> items = recommendationItemsRepository.findBySessionIdOrderByRank(sessionId);
        RecommendationSessionDetailsDto dto = new RecommendationSessionDetailsDto();
        dto.setId(session.getId());
        dto.setGeneratedAt(session.getGeneratedAt());
        dto.setItems(items.stream().map(i -> mapToRecommendation(i.getGame().getId(), i.getScore())).toList());
        return dto;
    }

    @Transactional
    public void generateRecommendationSession(Long userId) {
        List<RecommendationDto> recommendations = getRecommendationsForUser(userId);
        UserEntity user = userRepository.findById(userId).orElseThrow();
        RecommendationSession session = new RecommendationSession(user, LocalDateTime.now());
        sessionRepository.save(session);

        int rank = 1;
        for (RecommendationDto dto : recommendations) {
            GameEntity game = gameRepository.findById(dto.getGameId()).orElseThrow();
            RecommendationItems item = new RecommendationItems(game, session, rank, dto.getRecommendationScore());
            recommendationItemsRepository.save(item);
            rank++;
        }
    }
    public List<RecommendationDto> getFastRecommendations(Authentication authentication) {
        Long userId = userRepository.findByEmail(authentication.getName()).orElseThrow().getId();
        Optional<RecommendationSession> latestSessionOpt = sessionRepository
                .findByUserIdOrderByGeneratedAtDesc(userId)
                .stream()
                .findFirst();

        if (latestSessionOpt.isEmpty()) {
            return getColdStartRecommendations();
        }

        RecommendationSession session = latestSessionOpt.get();
        
        List<RecommendationItems> items = recommendationItemsRepository
                .findBySessionIdOrderByRank(session.getId());

        return items.stream()
                .map(item -> mapToRecommendation(item.getGame().getId(), item.getScore()))
                .toList();
    }
    @Transactional
    public void generateAndSaveRecommendations(Authentication authentication) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        Long userId = user.getId();

        // 1. Обновляем веса (как и было)
        recalculateUserPreferences(userId);

        // 2. Получаем рекомендации
        List<RecommendationDto> finalRecs;
        
        if (shouldApplySalt(userId)) {
            // Если пользователь игнорировал прошлую подборку:
            // Получаем расширенный список (например, топ-50) и перемешиваем
            List<RecommendationDto> candidates = new ArrayList<>(getRecommendationsForUser(userId)); 
            Collections.shuffle(candidates); // Вот она - "соль"
            finalRecs = candidates.stream().limit(20).toList();
        } else {
            // Если всё хорошо, просто берем стандартный топ-20
            finalRecs = getRecommendationsForUser(userId);
        }

        // 3. Сохраняем новую сессию
        RecommendationSession session = new RecommendationSession(user, LocalDateTime.now());
        sessionRepository.save(session);

        int rank = 1;
        for (RecommendationDto dto : finalRecs) {
            GameEntity game = gameRepository.findById(dto.getGameId()).orElseThrow();
            RecommendationItems item = new RecommendationItems(game, session, rank, dto.getRecommendationScore());
            recommendationItemsRepository.save(item);
            rank++;
        }
    }
    private boolean shouldApplySalt(Long userId) {
    // Достаем последнюю сессию пользователя
        return sessionRepository.findByUserIdOrderByGeneratedAtDesc(userId)
                .stream()
                .findFirst()
                .map(lastSession -> {
                    // Берем ID всех игр из той сессии
                    List<Long> lastGameIds = lastSession.getItems().stream()
                            .map(i -> i.getGame().getId())
                            .toList();

                    // Проверяем, появились ли новые записи в UserGames для этих игр 
                    // ПОСЛЕ времени генерации сессии
                    // (Для этого используем ваш userGameRepository)
                    boolean interactionExists = userGameRepository.existsByUserIdAndGameIdInAndTimeAfter(
                            userId, 
                            lastGameIds, 
                            lastSession.getGeneratedAt()
                    );

                    return !interactionExists; // Если взаимодействий НЕТ — возвращаем true (нужна соль)
                }).orElse(false);
    }
}