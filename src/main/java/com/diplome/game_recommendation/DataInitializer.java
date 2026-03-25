package com.diplome.game_recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.diplome.game_recommendation.repositories.GameRepository;
import com.diplome.game_recommendation.repositories.TagRepository;
import com.diplome.game_recommendation.services.gigachat.FilterTagService;
import com.diplome.game_recommendation.services.rawg.GameImportService;
import com.diplome.game_recommendation.services.rawg.TagImportService;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final GameRepository gameRepository;
    private final TagRepository tagRepository;
    private final GameImportService gameImportService;
    private final FilterTagService filterTagService;
    private final TagImportService tagImportService;

    @Bean
    public ApplicationRunner initData() {
        return args -> {

            if (gameRepository.count() > 0) {
                System.out.println("База уже содержит игры — пропускаем первичную загрузку.");
                return;
            }
            if (tagRepository.count() == 0) {
            System.out.println("БД пустая — начинаем первичную загрузку популярных тегов...");
            tagImportService.importImportantTags();
            System.out.println("Фильтруем теги через GigaChat...");
            try {
                filterTagService.filterTagsFromDb(); 
            } catch (Exception e) {
                System.err.println("Ошибка фильтрации тегов: " + e.getMessage());
                e.printStackTrace();
            }
            }
            
            System.out.println("Популярные теги загружены. Начинаем импорт игр по этим тегам...");

            gameImportService.importGamesByTags();

            System.out.println("Первичная загрузка тегов и игр завершена.");
        };
    }
}