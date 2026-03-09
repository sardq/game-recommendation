package com.diplome.game_recommendation.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.diplome.game_recommendation.core.configuration.Constants;
import com.diplome.game_recommendation.dtos.GameDto;
import com.diplome.game_recommendation.models.GameEntity;

@RestController
@RequestMapping(GameController.URL)
public class GameController {
    public static final String URL = Constants.API_URL +"/games";
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);
    //private final GameService service;
    //private final ModelMapper mapper;

    // public GameController(GameService service, ModelMapper mapper) {
    //     this.mapper = mapper;
    //     this.service = service;
    // }

    public GameDto toDto(GameEntity entity) {
        if (entity == null) {
            return null;
        }
        GameDto dto = mapper.map(entity, GameDto.class);
        return dto;
    }
    // @GetMapping
    // public List<GameDto> getAll() {
    //     logger.info("Получение всех оценок");
    //     var result = service.getAll(0, 40);
    //     return result.getContent()
    //             .stream()
    //             .map(this::toDto)
    //             .toList();
    // }

    // @GetMapping("/{id}")
    // public ResponseEntity<GameDto> get(@PathVariable Long id) {
    //     logger.info("Получение оценки id={}", id);
    //     return ResponseEntity.ok(toDto(service.get(id)));
    // }

}
