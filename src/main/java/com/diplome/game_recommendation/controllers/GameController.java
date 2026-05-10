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
import com.diplome.game_recommendation.dtos.TagDto;
import com.diplome.game_recommendation.helpers.configuration.Constants;
import com.diplome.game_recommendation.integration.CurrencyService;
import com.diplome.game_recommendation.integration.NewsService;
import com.diplome.game_recommendation.integration.PriceService;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.services.GameService;

@RestController
@RequestMapping(GameController.URL)
public class GameController {

    public static final String URL = Constants.API_URL + "/games";

    private final GameService service;
    private final PriceService priceService;
    private final NewsService newsService;
    private final CurrencyService currencyService;
    private final ModelMapper mapper;

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    public GameController(GameService service, PriceService priceService,CurrencyService currencyService, NewsService newsService, ModelMapper mapper) {
        this.service = service;
        this.priceService = priceService;
        this.currencyService = currencyService;
        this.newsService = newsService;
        this.mapper = mapper;
    }

    private GameDto toDto(GameEntity entity) {
        GameDto dto = mapper.map(entity, GameDto.class);
        
        if (dto.getLocalRating() == null) {
            dto.setLocalRating(entity.getLocalRating());
        }
        
        return dto;
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
        dto.setTrailerUrls(game.getTrailerUrls());
        dto.setWalkthroughUrls(game.getWalkthroughUrls());
        dto.setStoreLinks(game.getStoreLinks());
        dto.setScreenshotUrls(game.getScreenshotUrls());
        dto.setLocalRating(game.getLocalRating());
        dto.setLocalRatingCount(game.getLocalRatingCount());
        if (game.getRating() != null) {
            dto.setRating(BigDecimal.valueOf(game.getRating()));
        }

        if (game.getMetacriticRate() != null) {
            dto.setMetacriticRate(BigDecimal.valueOf(game.getMetacriticRate()));
        }

        dto.setPlaytime(game.getPlaytime());

        if (game.getGameTags() != null) {
            dto.setTags(game.getGameTags().stream()
                .map(gt -> {
                    TagDto t = new TagDto();
                    t.setId(gt.getTag().getId());
                    t.setName(gt.getTag().getName());
                    t.setNameRu(gt.getTag().getNameRu());
                    t.setSlug(gt.getTag().getSlug());
                    return t;
                }).toList());
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

    @PostMapping("/load/{dbId}")
    public ResponseEntity<Long> loadGame(@PathVariable Long dbId) {
        GameEntity game = service.loadGameIfNeeded(dbId);
        
        return ResponseEntity.ok(game.getId());
    }
    @GetMapping("/{id}")
    public ResponseEntity<GameDetailsDto> get(@PathVariable Long id) {
        GameEntity game = service.getGame(id);
        GameDetailsDto dto = toDetailsDto(game);
        dto.setDeals(priceService.getBestDeals(game.getName()));
        dto.setNews(newsService.getLatestNews(game.getName()));
        dto.setUsdRate(currencyService.getUsdRate()); 
        return ResponseEntity.ok(dto);
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