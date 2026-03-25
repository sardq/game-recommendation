package com.diplome.game_recommendation.services.rawg;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.diplome.game_recommendation.dtos.rawg.RawgGameDto;
import com.diplome.game_recommendation.dtos.rawg.RawgGameResponse;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.GameTag;
import com.diplome.game_recommendation.models.PlatformEnum;
import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.repositories.GameRepository;
import com.diplome.game_recommendation.repositories.GameTagRepository;
import com.diplome.game_recommendation.repositories.TagRepository;

@Service
public class GameImportService {

    private final RawgApiService rawgApiService;

    private final GameRepository gameRepository;
    private final TagRepository tagRepository;
    private final GameTagRepository gameTagRepository;

    public GameImportService(
            RawgApiService rawgApiService,
            GameRepository gameRepository,
            TagRepository tagRepository, 
            GameTagRepository gameTagRepository
    ) {
        this.rawgApiService = rawgApiService;
        this.gameRepository = gameRepository;
        this.gameTagRepository = gameTagRepository;
        this.tagRepository = tagRepository;
    }

    public void importGames(int page){

        RawgGameResponse response =
                rawgApiService.getGames(page);

        List<RawgGameDto> games = response.getResults();

        for(RawgGameDto dto : games){
            if(gameRepository.existsByRawgId(dto.getId())){
                continue;
            }
            GameEntity game = new GameEntity();
            game.setRawgId(dto.getId());
            game.setName(dto.getName());
            game.setDescription(dto.getDescription());
            game.setPosterUrl(dto.getBackground_image());
            game.setRating(dto.getRating());
            game.setPlaytime(dto.getPlaytime());
            if (dto.getPlatforms() != null) {
            Set<PlatformEnum> platforms = dto.getPlatforms().stream()
                .map(p -> {
                    Map<String, Object> platformMap = (Map<String, Object>) p.get("platform");
                    String name = ((String) platformMap.get("name")).toUpperCase()
                                    .replaceAll(" ", "_"); 
                    try {
                        return PlatformEnum.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            game.setPlatforms(platforms);
        }
            gameRepository.save(game);
        }

    }
    public void importGamesByTags() {
    List<TagEntity> importantTags = tagRepository.findByKeep(true);

    for (TagEntity tag : importantTags) {
        for (int page = 1; page <= 5; page++) {
            List<RawgGameDto> games = rawgApiService.getGamesByTag(tag.getSlug(), page);

            for (RawgGameDto dto : games) {

                GameEntity game = gameRepository.findByRawgId(dto.getId())
                        .orElseGet(() -> {
                            GameEntity g = createGameEntity(dto);
                            gameRepository.save(g);
                            return g;
                        });
                if (!gameTagRepository.existsByGameIdAndTagId(game.getId(), tag.getId())) {
                    GameTag gt = new GameTag(game, tag);
                    gameTagRepository.save(gt);
                }

            }
        }
    }
}

    private GameEntity createGameEntity(RawgGameDto dto) {
        GameEntity game = new GameEntity();
        game.setRawgId(dto.getId());
        game.setName(dto.getName());
        game.setDescription(dto.getDescription());
        game.setPosterUrl(dto.getBackground_image());
        game.setRating(dto.getRating());
        game.setPlaytime(dto.getPlaytime());
        
        if (dto.getPlatforms() != null) {
            Set<PlatformEnum> platforms = dto.getPlatforms().stream()
                    .map(p -> {
                        Map<String, Object> platform = (Map<String, Object>) p.get("platform");
                        String name = (String) platform.get("name");
                        try { return PlatformEnum.valueOf(name.toUpperCase()); } 
                        catch (Exception e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            game.setPlatforms(platforms);
        }

        return game;
    }

}