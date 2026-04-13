package com.diplome.game_recommendation.services;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.diplome.game_recommendation.dtos.rawg.PlatformWrapper;
import com.diplome.game_recommendation.dtos.rawg.RawgGameDetailsResponse;
import com.diplome.game_recommendation.integration.RawgApiService;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.InteractionEnum;
import com.diplome.game_recommendation.models.PlatformEnum;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.repositories.GameRepository;
import com.diplome.game_recommendation.repositories.UserGameRepository;
@Service
public class GameService {
     private static final Logger logger = LoggerFactory.getLogger(TagService.class);
    private static final String LOG_RESPONSE = "Ответ: {}";
    private final GameRepository gameRepository;
    private final RawgApiService rawgApiService;
    private final UserGameRepository userGameRepository;
    public GameService(GameRepository gameRepository, UserGameRepository userGameRepository, RawgApiService rawgApiService){
        this.gameRepository = gameRepository;
        this.userGameRepository = userGameRepository;
        this.rawgApiService = rawgApiService;
    }
    public Page<GameEntity> getGames(int page, int size){
        logger.info("Получение игр: {}, {}", page, size);
        var result = gameRepository.findAll(PageRequest.of(page, size));
        logger.info(LOG_RESPONSE, result);
        return result;
    }
     public Page<GameEntity> getGamesByTag(Long tagId, int page, int size){
        logger.info("Получение игр по тегу: {}, {}", page, size);
        var result = gameRepository.findByTagId(tagId, PageRequest.of(page, size));
        logger.info(LOG_RESPONSE, result);
        return result;
    }
    @Transactional(readOnly = true)
    public Page<GameEntity> getAllByFilters(String search, int page, int size) {
        logger.info("Фильтрация игр выполнена, page={}, pageSize={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<GameEntity> result;

         if (search != null && !search.isEmpty()) {
            result = gameRepository.filterBySearch(search, pageable);
        } else {
            result = gameRepository.findAll(pageable);
        }

        logger.info(LOG_RESPONSE, result);
        return result;
    }
    public GameEntity getGame(Long gameId){
        logger.info("Получение игры");
        var result = gameRepository.findById(gameId).orElse(null);
        logger.info(LOG_RESPONSE, result);
        return result;
    }
    public List<GameEntity> getFavorites(Long userId){
        return userGameRepository
                .findByUserIdAndInteraction(userId, InteractionEnum.Favorite)
                .stream()
                .map(UserGames::getGame)
                .toList();
    }
    public Page<GameEntity> getPopularGames(int page, int size){
        Pageable pageable = PageRequest.of(page, size);
        return gameRepository.findByOrderByReleaseDateDesc(pageable);
    }
    @Transactional
    public GameEntity loadGameIfNeeded(Long rawgId) {

        GameEntity existing = gameRepository.findById(rawgId).orElse(null);
        if (existing.getDescription() != null) {
            return existing;
        }

        RawgGameDetailsResponse response = rawgApiService.getGameDetails(existing.getRawgId());

        GameEntity game = mapToEntity(response);
        game.setId(rawgId);
        return gameRepository.save(game);
    }
    private GameEntity mapToEntity(RawgGameDetailsResponse r) {
        GameEntity game = new GameEntity();

        game.setName(r.getName());
        game.setRawgId(r.getId());
        game.setDescription(r.getDescription());

        if (r.getReleased() != null) {
            game.setReleaseDate(java.sql.Date.valueOf(r.getReleased()));
        }

        game.setRating(r.getRating());

        if (r.getMetacritic() != null) {
            game.setMetacriticRate(r.getMetacritic().doubleValue());
        }
        game.setPosterUrl(r.getBackground_image());
        game.setPlaytime(r.getPlaytime());

        if (r.getPlatforms() != null) {
            Set<PlatformEnum> platforms = r.getPlatforms().stream()
                .map((PlatformWrapper p) -> mapPlatform(p.getPlatform().getName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            game.setPlatforms(platforms);
        }

        return game;
    }
    private PlatformEnum mapPlatform(String name) {
    if (name == null) return null;

    return switch (name.toLowerCase()) {
        case "pc" -> PlatformEnum.PC;

        case "playstation 5", "playstation 4", "playstation 3" ->
                PlatformEnum.PLAYSTATION;

        case "xbox one", "xbox series s/x", "xbox 360" ->
                PlatformEnum.XBOX;

        case "nintendo switch" ->
                PlatformEnum.NINTENDO;

        default -> null; 
    };
}
}
