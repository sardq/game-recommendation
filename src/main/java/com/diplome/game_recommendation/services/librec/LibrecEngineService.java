package com.diplome.game_recommendation.services.librec;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
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
        File dataFile = null;
        try {
            dataFile = dataBuilder.buildFile();

            Configuration conf = new Configuration();
            conf.set("dfs.data.dir", dataFile.getParentFile().getAbsolutePath()); 
            conf.set("data.input.path", dataFile.getName());
            conf.set("data.model.format", "text");
            conf.set("data.column.format", "UIR");
            
            conf.set("rec.iterator.maximum", "50"); 
            conf.set("rec.factor.number", "10");    
            conf.set("rec.learnrate.bolddriver", "false");
            conf.set("rec.learnrate.value", "0.01");
            
            conf.set("rec.recommender.isranking", "true"); 
            conf.set("rec.recommender.ranking.topn", "20");

            TextDataModel dataModel = new TextDataModel(conf);
            dataModel.buildDataModel();

            RecommenderContext context = new RecommenderContext(conf, dataModel);

            Recommender recommender = new BiasedMFRecommender();
            recommender.recommend(context); 

            List<RecommendedItem> allRecs = recommender.getRecommendedList();
            
            List<RecommendedItem> userRecs = new ArrayList<>();
            String targetUserId = userId.toString();
            for (RecommendedItem item : allRecs) {
                if (item.getUserId().equals(targetUserId)) {
                    userRecs.add(item);
                }
            }

            userRecs.sort(Comparator.comparingDouble(RecommendedItem::getValue).reversed());

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
                dto.setLocalRating(game.getLocalRating());
                result.add(dto);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка работы LibRec: " + e.getMessage(), e);
        } finally {
            if (dataFile != null && dataFile.exists()) {
                dataFile.delete();
            }
        }
    }
}