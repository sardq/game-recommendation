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
            // 1. Создаем временный файл с данными
            dataFile = dataBuilder.buildFile();

            // 2. Настраиваем конфигурацию LibRec
            Configuration conf = new Configuration();
            conf.set("dfs.data.dir", dataFile.getParentFile().getAbsolutePath()); 
            conf.set("data.input.path", dataFile.getName());
            conf.set("data.model.format", "text");
            conf.set("data.column.format", "UIR");
            
            // Настройки нейросети (без них выдаст мусор)
            conf.set("rec.iterator.maximum", "50"); // Количество эпох обучения
            conf.set("rec.factor.number", "10");    // Скрытые факторы
            conf.set("rec.learnrate.bolddriver", "false");
            conf.set("rec.learnrate.value", "0.01");
            
            conf.set("rec.recommender.isranking", "true"); 
            conf.set("rec.recommender.ranking.topn", "20");

            // 3. Собираем модель данных
            TextDataModel dataModel = new TextDataModel(conf);
            dataModel.buildDataModel();

            // 4. Подготавливаем контекст
            RecommenderContext context = new RecommenderContext(conf, dataModel);

            // 5. ЗАПУСК АЛГОРИТМА (Ваш изначальный правильный синтаксис!)
            Recommender recommender = new BiasedMFRecommender();
            recommender.recommend(context); // Запускает весь процесс

            // 6. Получаем результаты
            List<RecommendedItem> allRecs = recommender.getRecommendedList();
            
            // 7. Фильтруем рекомендации для текущего пользователя
            List<RecommendedItem> userRecs = new ArrayList<>();
            String targetUserId = userId.toString();
            for (RecommendedItem item : allRecs) {
                if (item.getUserId().equals(targetUserId)) {
                    userRecs.add(item);
                }
            }

            // Сортируем от лучшего скора к худшему
            userRecs.sort(Comparator.comparingDouble(RecommendedItem::getValue).reversed());

            // 8. Конвертируем в наши DTO
            List<RecommendationDto> result = new ArrayList<>();
            for (RecommendedItem item : userRecs) {
                Long gameId = Long.parseLong(item.getItemId());
                double score = item.getValue(); // Предсказанный рейтинг LibRec

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
            throw new RuntimeException("Ошибка работы LibRec: " + e.getMessage(), e);
        } finally {
            // КРИТИЧЕСКИ ВАЖНО: Удаляем временный файл, чтобы не забить жесткий диск сервера
            if (dataFile != null && dataFile.exists()) {
                dataFile.delete();
            }
        }
    }
}