package com.diplome.game_recommendation.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.InteractionEnum;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.repositories.GameRepository;
import com.diplome.game_recommendation.repositories.UserGameRepository;
@Service
public class GameService {
     private static final Logger logger = LoggerFactory.getLogger(TagService.class);
    private static final String LOG_RESPONSE = "Ответ: {}";
    private final GameRepository gameRepository;
    private final UserGameRepository userGameRepository;
    public GameService(GameRepository gameRepository, UserGameRepository userGameRepository){
        this.gameRepository = gameRepository;
        this.userGameRepository = userGameRepository;
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
        return gameRepository.findTop5ByOrderByReleaseDateDesc(pageable);
    }
}
