package com.diplome.game_recommendation.controllers;

import java.util.List;

import org.modelmapper.ModelMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.diplome.game_recommendation.core.configuration.Constants;
import com.diplome.game_recommendation.dtos.GameDto;
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

    @GetMapping("/{id}")
    public ResponseEntity<GameDto> get(@PathVariable Long id) {
        logger.info("Получение игры id={}", id);

        return ResponseEntity.ok(toDto(service.getGame(id)));
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
    public List<GameDto> popular() {
        logger.info("Популярные игры");

        return service.getPopularGames()
                .getContent()
                .stream()
                .map(this::toDto)
                .toList();
    }
}