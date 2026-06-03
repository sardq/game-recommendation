package com.diplome.game_recommendation.services.gigachat;

import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.repositories.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
    "rawg.key=test_key",
    "video.key=test_key",
    "news.key=test_key",
    "gigachat.authorization-key=MDE5ZDIwOWQtODk5Zi03YTk2LWE4MzMtZTQ1OTkxNjExMWNkOjY5ZTlmZDY4LTE2ZGMtNDY5NC04MDkzLTliMDllNDhjNWEyNA==",
    "spring.mail.username=test@mail.ru",
    "spring.mail.password=test",
    "minio.url=http://localhost:9000",
    "minio.access-key=minioadmin",
    "minio.secret-key=minioadmin",
    "minio.bucket=test",
    "spring.quartz.job-store-type=memory" 
}) 
@ActiveProfiles("dev") 
public class GigachatIntegrationAccuracyTest {

    @Autowired
    private FilterTagService filterTagService;

    @MockitoBean 
    private TagRepository tagRepository;

    @Test
    @DisplayName("ЭКСПЕРИМЕНТ: Оценка реальной точности GigaChat на выборке N=50")
    void evaluateRealAccuracy() throws Exception {
        Map<String, Boolean> goldStandard = getGoldStandard();
        List<TagEntity> testBatch = new ArrayList<>();
        
        goldStandard.forEach((name, keep) -> {
            testBatch.add(new TagEntity(name, "Game related tag: " + name, null, null, false, name.toLowerCase()));
        });

        System.out.println("Отправка запроса к GigaChat... Это может занять время.");
        List<TagEntity> results = filterTagService.filterTags(testBatch);

        int correct = 0;
        int falsePositives = 0; 
        int falseNegatives = 0; 

        for (TagEntity tag : testBatch) {
            Boolean predicted = tag.getKeep();
            Boolean actual = goldStandard.get(tag.getName());

            if (actual.equals(predicted)) {
                correct++;
            } else {
                if (predicted) falsePositives++; else falseNegatives++;
                System.out.println("MISMATCH: [" + tag.getName() + "] | Модель: " + predicted + " | Ожидалось: " + actual);
            }
        }

        double accuracy = (double) correct / goldStandard.size();
        
        System.out.println("Инструмент: GigaChat API");
        System.out.println("Размер выборки: " + goldStandard.size() + " тегов");
        System.out.println("Верных ответов: " + correct);
        System.out.println("Ложноположительных: " + falsePositives);
        System.out.println("Ложноотрицательных: " + falseNegatives);
        System.out.println("ИТОГОВАЯ ТОЧНОСТЬ (ACCURACY): " + String.format("%.2f", accuracy * 100) + "%");

        verify(tagRepository, atLeastOnce()).save(any());

        assertTrue(accuracy >= 0.80, "Точность классификации ниже допустимой!");
    }

    private Map<String, Boolean> getGoldStandard() {
        Map<String, Boolean> map = new HashMap<>();
        String[] valid = {"RPG", "Action", "Horror", "Souls-like", "Stealth", "Indie", "Sci-fi", "Sandbox", 
            "Cyberpunk", "Strategy", "Survival", "Noir", "Puzzle", "Platformer", "Roguelike", 
            "Turn-based", "Isometric", "Co-op", "Anime", "Gore", "Dungeon", "Viking", "Zombies", "JRPG", "Crafting"};
        
        String[] garbage = {"v.1.0.5", "Win 11", "Sale", "NVIDIA", "RTX On", "Pre-Order", "4k Texture", "Bug fix", 
            "Intel i9", "SteamDB", "Epic Store", "DirectX12", "16GB RAM", "Ver. Final", "Deluxe", 
            "Twitch", "Season 1", "EULA", "Free2play", "Bonus", "Redeem", "Gold Edit.", "Launcher", "Play", "Online"};
        
        for (String s : valid) map.put(s, true);
        for (String s : garbage) map.put(s, false);
        return map;
    }
}