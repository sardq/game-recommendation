package com.diplome.game_recommendation.services.gigachat;

import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.repositories.TagRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilterTagService {

    private final GigachatService gigachatClient; 
    private final ObjectMapper objectMapper;
    private final TagRepository tagRepository;

    private static final int BATCH_SIZE = 50;

    public List<TagEntity> filterTags(List<TagEntity> tags) throws Exception {

        List<TagEntity> filteredTags = new ArrayList<>();

        for (int i = 0; i < tags.size(); i += BATCH_SIZE) {
            List<TagEntity> batch = tags.subList(i, Math.min(i + BATCH_SIZE, tags.size()));

            String prompt = buildPrompt(batch);

            String responseJson = gigachatClient.ask(prompt);

            List<Map<String, Object>> results = objectMapper.readValue(
                    responseJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            for (Map<String, Object> r : results) {
                Boolean keep = (Boolean) r.get("keep");
                String name = (String) r.get("name");

                batch.stream()
                    .filter(t -> t.getName().equals(name))
                    .findFirst()
                    .ifPresent(t -> {
                        t.setKeep(keep);
                        tagRepository.save(t); // сохраняем в базу
                    });

                if (Boolean.TRUE.equals(keep)) {
                    batch.stream()
                        .filter(t -> t.getName().equals(name))
                        .findFirst()
                        .ifPresent(filteredTags::add);
                }
            }
        }

        return filteredTags;
    }

    private String buildPrompt(List<TagEntity> batch) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ты эксперт по классификации видеоигр. У тебя есть список тегов с названием и описанием. ");
        sb.append("Твоя задача:\n");
        sb.append("1. Убрать теги 18+ или интимного характера(keep = false).\n");
        sb.append("2. Убрать мусорные или неинформативные теги (keep = false).\n");
        sb.append("3. Стандартизировать и унифицировать названия тегов (нижний регистр, корректное написание, без спецсимволов).\n");
        sb.append("Верни только JSON массив без пояснений, без текста, без #, без какого либо лишнего символа для каждой игры в формате [{\"name\":\"<tag>\",\"description\":\"<description>\",\"keep\":true/false}, ...]\n");
        sb.append("Теги:\n[");

        String tagsJson = batch.stream()
            .map(t -> String.format(
                "{\"name\":\"%s\",\"description\":\"%s\"}",
                escapeJson(t.getName()),
                escapeJson(t.getDescription() != null ? t.getDescription() : "")
            ))
            .collect(Collectors.joining(","));

        sb.append(tagsJson);
        sb.append("]");

        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ");
    }

    public List<TagEntity> filterTagsFromDb() throws Exception {
        List<TagEntity> allTags = tagRepository.findAll();
        return filterTags(allTags);
    }
}