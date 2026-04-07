package com.diplome.game_recommendation.services;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    // ВЕРНУЛИ LIBREC
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

    // =========================================================================
    // 1. ПЕРЕСЧЕТ ПРЕДПОЧТЕНИЙ (Вызывается по кнопке или расписанию)
    // =========================================================================
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

        for (Map.Entry<TagEntity, Double> entry : tagScores.entrySet()) {
            if (entry.getValue() > 0) { 
                UserPreference pref = new UserPreference();
                pref.setUser(user);
                pref.setTag(entry.getKey());
                pref.setPreferenceWeight(entry.getValue().doubleValue());
                userPreferenceRepository.save(pref);
            }
        }
    }
    // =========================================================================
    // 2. ГЛАВНЫЙ МЕТОД: МНОГОСТУПЕНЧАТАЯ СИСТЕМА (Ансамбль CB + LibRec)
    // =========================================================================
    public List<RecommendationDto> getRecommendationsForUser(Authentication authentication) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElse(new UserEntity());
        int interactions = userGameRepository.countByUserId(user.getId());

        // СТУПЕНЬ 1: Холодный старт
        if (interactions < 3) {
            return getColdStartRecommendations();
        }

        // СТУПЕНЬ 2: Контентная фильтрация (Скор 0.0 - 1.0)
        Map<Long, Double> contentScores = getContentBasedScores(user.getId());

        // СТУПЕНЬ 3: Коллаборативная фильтрация через LibRec
        List<RecommendationDto> librecRecs = librecEngineService.recommend(user.getId());
        Map<Long, Double> collabScores = new HashMap<>();
        for (RecommendationDto rec : librecRecs) {
            collabScores.put(rec.getGameId(), rec.getRecommendationScore() / 10.0);
        }


        // СТУПЕНЬ 4: Смешивание оценок (Blending/Ensemble)
        Map<Long, Double> finalScores = new HashMap<>();
        List<GameEntity> allGames = gameRepository.findAll();

        for (GameEntity game : allGames) {
            Long gameId = game.getId();

            double cScore = contentScores.getOrDefault(gameId, 0.0);
            double cfScore = collabScores.getOrDefault(gameId, 0.0);

            // Веса алгоритмов (Например: 60% контент, 40% LibRec). 
            // Это можно вынести в настройки.
            double hybridScore = (cScore * 0.6) + (cfScore * 0.4);
            
            if (hybridScore > 0) {
                finalScores.put(gameId, hybridScore);
            }
        }

        // СТУПЕНЬ 5: Ранжирование и маппинг
        return finalScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(20)
                .map(e -> mapToRecommendation(e.getKey(), e.getValue()))
                .toList();
    }
    public List<RecommendationDto> getRecommendationsForUser(Long userId) {
        int interactions = userGameRepository.countByUserId(userId);

        // СТУПЕНЬ 1: Холодный старт
        if (interactions < 3) {
            return getColdStartRecommendations();
        }

        // СТУПЕНЬ 2: Контентная фильтрация (Скор 0.0 - 1.0)
        Map<Long, Double> contentScores = getContentBasedScores(userId);

        // СТУПЕНЬ 3: Коллаборативная фильтрация через LibRec
        List<RecommendationDto> librecRecs = librecEngineService.recommend(userId);
        Map<Long, Double> collabScores = new HashMap<>();
        for (RecommendationDto rec : librecRecs) {
            // Если LibRec выдает оценку от 1 до 10, делим на 10 для нормализации (0..1)
            // Если он уже выдает вероятности от 0 до 1, деление не нужно.
            collabScores.put(rec.getGameId(), rec.getRecommendationScore() / 10.0);
        }

        // Получаем список сыгранных игр, чтобы исключить их из выдачи
        Set<Long> playedGames = userGameRepository.findByUserId(userId).stream()
                .map(ug -> ug.getGame().getId())
                .collect(Collectors.toSet());

        // СТУПЕНЬ 4: Смешивание оценок (Blending/Ensemble)
        Map<Long, Double> finalScores = new HashMap<>();
        List<GameEntity> allGames = gameRepository.findAll();

        for (GameEntity game : allGames) {
            Long gameId = game.getId();
            if (playedGames.contains(gameId)) continue; 

            double cScore = contentScores.getOrDefault(gameId, 0.0);
            double cfScore = collabScores.getOrDefault(gameId, 0.0);

            // Веса алгоритмов (Например: 60% контент, 40% LibRec). 
            // Это можно вынести в настройки.
            double hybridScore = (cScore * 0.6) + (cfScore * 0.4);
            
            if (hybridScore > 0) {
                finalScores.put(gameId, hybridScore);
            }
        }

        // СТУПЕНЬ 5: Ранжирование и маппинг
        return finalScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(20)
                .map(e -> mapToRecommendation(e.getKey(), e.getValue()))
                .toList();
    }
    // =========================================================================
    // 3. КОНТЕНТНАЯ ФИЛЬТРАЦИЯ (Векторная модель)
    // =========================================================================
    private Map<Long, Double> getContentBasedScores(Long userId) {
    List<UserPreference> prefs = userPreferenceRepository.findByUserId(userId);
    Map<Long, Double> userVector = prefs.stream().collect(Collectors.toMap(
            p -> p.getTag().getId(),
            p -> p.getPreferenceWeight().doubleValue()
    ));

    double userNormSq = userVector.values().stream().mapToDouble(w -> w * w).sum();
    double userNorm = Math.sqrt(userNormSq);

    List<GameEntity> games = gameRepository.findAll();
    
    // РЕШЕНИЕ ПРОБЛЕМЫ N+1: Достаем все связи тегов ОДНИМ запросом
    List<GameTag> allGameTags = gameTagRepository.findAll();
    
    // Группируем их в памяти: Map<GameId, List<GameTag>>
    Map<Long, List<GameTag>> tagsByGameId = allGameTags.stream()
            .collect(Collectors.groupingBy(gt -> gt.getGame().getId()));

    Map<Long, Double> scores = new HashMap<>();

    for (GameEntity game : games) {
        // Берем теги из памяти (Map), а не из базы данных!
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

    // =========================================================================
    // 4. УТИЛИТЫ И ПРОЧЕЕ
    // =========================================================================
    public List<RecommendationDto> getColdStartRecommendations() {
        Page<GameEntity> games = gameRepository.findByRatingGreaterThanEqual(8.0, PageRequest.of(0, 20));
        return games.stream()
                .map(g -> mapToRecommendation(g.getId(), g.getRating() / 10.0)) // переводим в 0-1
                .toList();
    }

    public List<RecommendationDto> getSimilarGames(Long gameId) {
    GameEntity targetGame = gameRepository.findById(gameId).orElseThrow();
    Set<TagEntity> targetTagSet = gameTagRepository.findByGameId(targetGame.getId()).stream()
            .map(GameTag::getTag).collect(Collectors.toSet());

    List<GameEntity> allGames = gameRepository.findAll();
    
    // РЕШЕНИЕ ПРОБЛЕМЫ N+1
    List<GameTag> allGameTags = gameTagRepository.findAll();
    Map<Long, List<GameTag>> tagsByGameId = allGameTags.stream()
            .collect(Collectors.groupingBy(gt -> gt.getGame().getId()));

    Map<Long, Double> similarity = new HashMap<>(); 
    for (GameEntity g : allGames) {
        if (g.getId().equals(gameId)) continue;
        
        // Достаем из памяти
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
        
        // Вычисляем % близости
        int percentage = (int) Math.round(rawScore * 100);
        percentage = Math.max(0, Math.min(100, percentage)); 
        
        dto.setMatchPercentage(percentage); 

        return dto;
    }

    // =========================================================================
    // 5. РАБОТА С СЕССИЯМИ БД
    // =========================================================================
   public List<RecommendationSessionDto> getUserSessions(Authentication authentication) {
    Long userId = userRepository.findByEmail(authentication.getName()).orElseThrow().getId();
    return sessionRepository.findByUserIdOrderByGeneratedAtDesc(userId).stream()
            .map(s -> {
                RecommendationSessionDto dto = new RecommendationSessionDto();
                dto.setId(s.getId());
                dto.setGeneratedAt(s.getGeneratedAt());
                // Считаем количество игр в этой сессии
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
        
        // Маппим сохраненные айтемы в список игр для отображения
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
        // Ищем самую свежую сессию пользователя
        Optional<RecommendationSession> latestSessionOpt = sessionRepository
                .findByUserIdOrderByGeneratedAtDesc(userId)
                .stream()
                .findFirst();

        if (latestSessionOpt.isEmpty()) {
            // Если сессий еще нет, возвращаем холодный старт
            return getColdStartRecommendations();
        }

        RecommendationSession session = latestSessionOpt.get();
        
        // Достаем сохраненные игры из БД
        List<RecommendationItems> items = recommendationItemsRepository
                .findBySessionIdOrderByRank(session.getId());

        // Переводим в DTO и отдаем на фронтенд
        return items.stream()
                .map(item -> mapToRecommendation(item.getGame().getId(), item.getScore()))
                .toList();
    }
    @Transactional
    public void generateAndSaveRecommendations(Authentication authentication) {
        Long userId = userRepository.findByEmail(authentication.getName()).orElseThrow().getId();
        // 1. Сначала пересчитываем профиль (любимые теги)
        recalculateUserPreferences(userId);

        // 2. Запускаем ТЯЖЕЛЫЙ алгоритм (Ансамбль LibRec + Content-based)
        // (Это тот самый метод, который мы писали в прошлом ответе)
        List<RecommendationDto> calculatedRecs = getRecommendationsForUser(userId);

        // 3. Сохраняем результаты в базу данных (создаем сессию)
        UserEntity user = userRepository.findById(userId).orElseThrow();
        RecommendationSession session = new RecommendationSession(user, LocalDateTime.now());
        sessionRepository.save(session);

        int rank = 1;
        for (RecommendationDto dto : calculatedRecs) {
            GameEntity game = gameRepository.findById(dto.getGameId()).orElseThrow();
            RecommendationItems item = new RecommendationItems(game, session, rank, dto.getRecommendationScore());
            recommendationItemsRepository.save(item);
            rank++;
        }
    }
}