package com.diplome.game_recommendation.services.librec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.diplome.game_recommendation.dtos.RecommendationDto;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.repositories.GameRepository;

import net.librec.conf.Configuration;
import net.librec.data.model.TextDataModel;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.cf.rating.BiasedMFRecommender;

@Service
public class LibrecEngineService {

    private final LibrecDataBuilder dataBuilder;
    private final GameRepository gameRepository;

    public LibrecEngineService(LibrecDataBuilder dataBuilder,
                               GameRepository gameRepository) {
        this.dataBuilder = dataBuilder;
        this.gameRepository = gameRepository;
    }

    public List<RecommendationDto> recommend(Long userId) {
        try {
            File dataFile = dataBuilder.buildFile();

            Configuration conf = new Configuration();
            conf.set("data.input.path", dataFile.getAbsolutePath());
            conf.set("data.model.format", "text");
            conf.set("data.column.format", "UIR");
            conf.set("rec.recommender.class", BiasedMFRecommender.class.getName());

            TextDataModel dataModel = new TextDataModel(conf);
            dataModel.buildDataModel();

            RecommenderContext context = new RecommenderContext(conf, dataModel);

            Recommender recommender = new BiasedMFRecommender();
            recommender.recommend(context);
            List<RecommendedItem> allRecs = recommender.getRecommendedList();
            List<RecommendedItem> userRecs = new ArrayList<>();
            for (RecommendedItem item : allRecs) {
                if (item.getUserId().equals(userId.toString())) {
                    userRecs.add(item);
                }
            }
            List<RecommendationDto> result = new ArrayList<>();
            for (RecommendedItem item : userRecs) {
                Long gameId = Long.parseLong(item.getItemId());
                double score = item.getValue();

                GameEntity game = gameRepository.findById(gameId).orElse(null);
                if (game == null) continue;

                RecommendationDto dto = new RecommendationDto();
                dto.setGameId(gameId);
                dto.setName(game.getName());
                dto.setPosterUrl(game.getPosterUrl());
                dto.setRating(game.getRating());
                dto.setRecommendationScore(score);

                result.add(dto);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}