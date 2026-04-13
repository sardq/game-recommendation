package com.diplome.game_recommendation.controllers;

import java.math.BigDecimal;
import java.util.List;

import org.modelmapper.ModelMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.diplome.game_recommendation.dtos.GameDetailsDto;
import com.diplome.game_recommendation.dtos.GameDto;
import com.diplome.game_recommendation.helpers.configuration.Constants;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.services.GameService;

@RestController
@RequestMapping(GameController.URL)
public class GameController {

    public static final String URL = Constants.API_URL + "/games";

    private final GameService service;
    private final ModelMapper mapper;

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    public GameController(GameService service, ModelMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    private GameDto toDto(GameEntity entity) {
        return mapper.map(entity, GameDto.class);
    }
    private GameDetailsDto toDetailsDto(GameEntity game) {
        if (game == null) return null;

        GameDetailsDto dto = new GameDetailsDto();

        dto.setId(game.getId());
        dto.setName(game.getName());
        dto.setDescription(game.getDescription());

        if (game.getReleaseDate() != null) {
            dto.setReleaseDate(
                game.getReleaseDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
            );
        }

        if (game.getPlatforms() != null) {
            dto.setPlatforms(
                game.getPlatforms()
                    .stream()
                    .map(Enum::name)
                    .toList()
            );
        }

        dto.setPosterUrl(game.getPosterUrl());

        if (game.getRating() != null) {
            dto.setRating(BigDecimal.valueOf(game.getRating()));
        }

        if (game.getMetacriticRate() != null) {
            dto.setMetacriticRate(BigDecimal.valueOf(game.getMetacriticRate()));
        }

        dto.setPlaytime(game.getPlaytime());

        if (game.getGameTags() != null) {
            dto.setTags(
                game.getGameTags()
                    .stream()
                    .map(gt -> gt.getTag().getName())
                    .toList()
            );
        }

        return dto;
    }
    @GetMapping
    public List<GameDto> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        logger.info("Получение игр page={}, size={}", page, size);

        return service.getGames(page, size)
                .getContent()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping("/load/{rawgId}")
    public ResponseEntity<Long> loadGame(@PathVariable Long rawgId) {
        GameEntity game = service.loadGameIfNeeded(rawgId);
        return ResponseEntity.ok(game.getId());
    }
    @GetMapping("/{id}")
    public ResponseEntity<GameDetailsDto> get(@PathVariable Long id) {
        GameEntity game = service.getGame(id);
        return ResponseEntity.ok(toDetailsDto(game));
    }

    @GetMapping("/filter")
    public List<GameDto> filter(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        logger.info("Фильтрация игр search={}", search);

        return service.getAllByFilters(search, page, size)
                .getContent()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/tag/{tagId}")
    public List<GameDto> getByTag(
            @PathVariable Long tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        logger.info("Игры по тегу tagId={}", tagId);

        return service.getGamesByTag(tagId, page, size)
                .getContent()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/favorites/{userId}")
    public List<GameDto> favorites(@PathVariable Long userId) {
        logger.info("Избранные игры userId={}", userId);

        return service.getFavorites(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/popular")
    public List<GameDto> popular( 
        @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.info("Популярные игры");

        return service.getPopularGames(page, size)
                .getContent()
                .stream()
                .map(this::toDto)
                .toList();
    }
}