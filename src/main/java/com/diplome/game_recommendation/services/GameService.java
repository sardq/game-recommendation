package com.diplome.game_recommendation.services;

import java.util.ArrayList;
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
import com.diplome.game_recommendation.dtos.rawg.RawgStoreResponse;
import com.diplome.game_recommendation.integration.RawgApiService;
import com.diplome.game_recommendation.integration.VideoApiService;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.InteractionEnum;
import com.diplome.game_recommendation.models.PlatformEnum;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.repositories.GameRepository;
import com.diplome.game_recommendation.repositories.UserGameRepository;
@Service
public class GameService {
    private final GameRepository gameRepository;
    private final RawgApiService rawgApiService;
    private final VideoApiService videoApiService;
    private final UserGameRepository userGameRepository;
    public GameService(GameRepository gameRepository, UserGameRepository userGameRepository, RawgApiService rawgApiService, VideoApiService videoApiService){
        this.gameRepository = gameRepository;
        this.userGameRepository = userGameRepository;
        this.rawgApiService = rawgApiService;
        this.videoApiService = videoApiService;
    }
    public Page<GameEntity> getGames(int page, int size){
        var result = gameRepository.findAll(PageRequest.of(page, size));
        return result;
    }
     public Page<GameEntity> getGamesByTag(Long tagId, int page, int size){
        var result = gameRepository.findByTagId(tagId, PageRequest.of(page, size));
        return result;
    }
    @Transactional(readOnly = true)
    public Page<GameEntity> getAllByFilters(String search, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<GameEntity> result;

         if (search != null && !search.isEmpty()) {
            result = gameRepository.filterBySearch(search, pageable);
        } else {
            result = gameRepository.findAll(pageable);
        }

        return result;
    }
    public GameEntity getGame(Long gameId){
        var result = gameRepository.findById(gameId).orElse(null);
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
    public GameEntity loadGameIfNeeded(Long dbId) {
        GameEntity existing = gameRepository.findById(dbId)
                .orElseThrow(() -> new RuntimeException("Игра не найдена"));
        
        if (existing.getDescription() != null) {
            return existing;
        }

        RawgGameDetailsResponse response = rawgApiService.getGameDetails(existing.getRawgId());
        List<RawgStoreResponse.StoreResult> stores = rawgApiService.getGameStores(existing.getRawgId());
        List<String> screenshots = rawgApiService.getGameScreenshots(existing.getRawgId());
        List<String> trailers = videoApiService.searchVideos(existing.getName(), "gameplay trailer", 3);
        List<String> walkthroughs = videoApiService.searchVideos(existing.getName(), "Прохождение", 3);

        updateEntityWithExternalData(existing, response);

        existing.setScreenshotUrls(new ArrayList<>(screenshots)); 
        existing.setTrailerUrls(new ArrayList<>(trailers));
        existing.setWalkthroughUrls(new ArrayList<>(walkthroughs));
         existing.setStoreLinks(stores.stream()
            .map(s -> s.getUrl())
            .collect(Collectors.toList()));

        return gameRepository.save(existing);
    }
    private void updateEntityWithExternalData(GameEntity existing, RawgGameDetailsResponse r) {
        existing.setDescription(r.getDescription());

        if (r.getReleased() != null) {
            existing.setReleaseDate(java.sql.Date.valueOf(r.getReleased()));
        }

        existing.setRating(r.getRating());

        if (r.getMetacritic() != null) {
            existing.setMetacriticRate(r.getMetacritic().doubleValue());
        }
        
        existing.setPosterUrl(r.getBackground_image());
        existing.setPlaytime(r.getPlaytime());

        if (r.getPlatforms() != null) {
            Set<PlatformEnum> platforms = r.getPlatforms().stream()
                .map(p -> mapPlatform(p.getPlatform().getName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            existing.setPlatforms(platforms);
        }
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
    @Transactional
    public void updateLocalRating(Long gameId) {
        Double average = userGameRepository.getAverageRatingForGame(gameId);
        Integer count = userGameRepository.getCountOfRatingsForGame(gameId);
        
        GameEntity game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Игра не найдена"));
                
        game.setLocalRating(average != null ? average : 0.0);
        game.setLocalRatingCount(count != null ? count : 0);
        
        gameRepository.save(game);
    }
}
