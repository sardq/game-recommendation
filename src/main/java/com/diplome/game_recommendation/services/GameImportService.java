package com.diplome.game_recommendation.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.dtos.RawgGameDto;
import com.diplome.game_recommendation.dtos.RawgGameResponse;
import com.diplome.game_recommendation.repositories.GameRepository;

@Service
public class GameImportService {

    private final RawgApiService rawgApiService;

    private final GameRepository gameRepository;

    public GameImportService(
            RawgApiService rawgApiService,
            GameRepository gameRepository
    ) {
        this.rawgApiService = rawgApiService;
        this.gameRepository = gameRepository;
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
            game.setPosterUrl(dto.getBackground_image());
            game.setRating(dto.getRating());

            gameRepository.save(game);
        }

    }

}