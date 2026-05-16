// package com.diplome.game_recommendation.services.gigachat;

// import com.diplome.game_recommendation.integration.GigachatService;
// import com.diplome.game_recommendation.models.TagEntity;
// import com.diplome.game_recommendation.repositories.TagRepository;
// import com.fasterxml.jackson.core.type.TypeReference;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;

// import java.util.*;
// import java.util.stream.Collectors;

// @Service
// @RequiredArgsConstructor
// public class FilterTagService {

//     private final GigachatService gigachatClient; 
//     private final ObjectMapper objectMapper;
//     private final TagRepository tagRepository;

//     private static final int BATCH_SIZE = 50;

//     public List<TagEntity> filterTags(List<TagEntity> tags) throws Exception {
//     List<TagEntity> uniqueTags = tags.stream()
//         .collect(Collectors.toMap(TagEntity::getName, t -> t, (t1, t2) -> t1))
//         .values()
//         .stream()
//         .toList();
//         List<TagEntity> filteredTags = new ArrayList<>();

//         for (int i = 0; i < uniqueTags.size(); i += BATCH_SIZE) {
//             List<TagEntity> batch = uniqueTags.subList(i, Math.min(i + BATCH_SIZE, uniqueTags.size()));

//             String prompt = buildPrompt(batch);

//             String responseJson = gigachatClient.ask(prompt);

//            System.out.println("RAW RESPONSE: " + responseJson);

//             int start = responseJson.indexOf("[");
//             int end = responseJson.lastIndexOf("]");

//             if (start < 0 || end < 0 || end <= start) {
//                 throw new RuntimeException("Не удалось найти JSON в ответе: " + responseJson);
//             }

//             String jsonPart = responseJson.substring(start, end + 1);
//             String fixedJson = fixJson(jsonPart);

//             List<Map<String, Object>> results = objectMapper.readValue(
//                     fixedJson,
//                     new TypeReference<List<Map<String, Object>>>() {}
//             );

//             for (Map<String, Object> r : results) {
//                 Boolean keep = (Boolean) r.get("keep");
//                 String name = (String) r.get("name");

//                 batch.stream()
//                     .filter(t -> t.getName().equals(name))
//                     .findFirst()
//                     .ifPresent(t -> {
//                         t.setKeep(keep);
//                         tagRepository.save(t); 
//                     });

//                 if (Boolean.TRUE.equals(keep)) {
//                     batch.stream()
//                         .filter(t -> t.getName().equals(name))
//                         .findFirst()
//                         .ifPresent(filteredTags::add);
//                 }
//             }
//         }

//         return filteredTags;
//     }

//     private String buildPrompt(List<TagEntity> batch) {
//         StringBuilder sb = new StringBuilder();
//         sb.append("""
//             Ты должен вернуть ТОЛЬКО JSON.

//             Формат строго:
//             [
//             {"name": "string", "description": "string", "keep": true или false}
//             ]
//             Пример корректного JSON, который ты должен вернуть:
//             [
//             {"name": "RPG", "description": "", "keep": true},
//             {"name": "battle", "description": "", "keep": false}
//             ]
//             Правила:
//                  Оставлять ТОЛЬКО:
//                 - жанры (RPG, FPS, Strategy)
//                 - сеттинг (Fantasy, Sci-Fi, Medieval)
//                 - механики (Crafting, Stealth, Co-op)
//                 - стиль игры (Open World, Sandbox)

//                 Ставить keep=false если:
//                 - слишком общий (battle, character, story, friends)
//                 - технический (steam, controller, cloud, editor)
//                 - платформа (console, mobile)
//                 - год, числа (1990's)
//                 - дубликаты (mmo, mmorpg → оставить только mmorpg)
//                 - субъективные (beautiful, fun, epic, emotional)
//                 - одиночные слова без смысла (online, game, play)
//                 - steam-trading cards, steam-achievements, steam-cloud
//             Если сомневаешься → keep=false
//             ВАЖНО: ты должен возвращать JSON **только с тегами, которые я передал**. 
//             НЕЛЬЗЯ придумывать новые теги. 
//             Если тег из моего списка не подходит → ставь keep=false, 
//             но **не добавляй никаких других тегов**.
//             Теги, которые обязательно нужно обработать:
//             [
//             """);

//         String tagsJson = batch.stream()
//             .map(t -> String.format(
//                 "{\"name\":\"%s\",\"description\":\"%s\"}",
//                 escapeJson(t.getName()),
//                 escapeJson(t.getDescription() != null ? t.getDescription() : "")
//             ))
//             .collect(Collectors.joining(","));

//         sb.append(tagsJson);
//         sb.append("]");

//         return sb.toString();
//     }

//     private String escapeJson(String s) {
//         if (s == null) return "";
//         return s.replace("\"", "\\\"").replace("\n", " ");
//     }

//     public List<TagEntity> filterTagsFromDb() throws Exception {
//         List<TagEntity> allTags = tagRepository.findAll();
//         return filterTags(allTags);
//     }
//     private String fixJson(String json) {
//     json = json.replaceAll("//.*", "");

//     json = json.replaceAll("\"?keep\\s*:\\s*true\"?", "\"keep\": true");
//     json = json.replaceAll("\"?keep\\s*:\\s*false\"?", "\"keep\": false");

//     json = json.replaceAll(",\\s*\"\\w+\"(?=,|})", "");

//     json = json.replaceAll("\\s*:\\s*", ":");
//     json = json.replaceAll("\\s*,\\s*", ",");

//     json = json.replaceAll("}\\s*\\{", "},{");

//     json = json.replaceAll(",\\s*,", ",");
//     json = json.replaceAll(",\\s*]", "]");

//     json = json.replace("\n", " ").replace("\r", " ");

//     return json.trim();
// }
// }