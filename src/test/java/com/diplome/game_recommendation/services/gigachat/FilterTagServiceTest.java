package com.diplome.game_recommendation.services.gigachat;

import com.diplome.game_recommendation.integration.GigachatService;
import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.repositories.TagRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilterTagServiceTest {

    @Mock
    private GigachatService gigachatClient;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FilterTagService filterTagService;

    private List<TagEntity> testTags;
    private TagEntity tag1;
    private TagEntity tag2;
    private TagEntity tag3;
    private TagEntity tag4;

    @BeforeEach
    void setUp() {
        tag1 = new TagEntity();
        tag1.setId(1L);
        tag1.setName("RPG");
        tag1.setDescription("Role Playing Games");
        tag1.setKeep(false); // Initially false

        tag2 = new TagEntity();
        tag2.setId(2L);
        tag2.setName("battle");
        tag2.setDescription("Combat mechanics");
        tag2.setKeep(false);

        tag3 = new TagEntity();
        tag3.setId(3L);
        tag3.setName("Fantasy");
        tag3.setDescription("Fantasy setting");
        tag3.setKeep(false);

        tag4 = new TagEntity();
        tag4.setId(4L);
        tag4.setName("Stealth");
        tag4.setDescription("Stealth mechanics");
        tag4.setKeep(false);

        testTags = Arrays.asList(tag1, tag2, tag3, tag4);
    }

    @Test
    void filterTags_ShouldFilterAndSaveTagsInBatches() throws Exception {
        // Arrange
        String responseJson = """
            [
                {"name": "RPG", "description": "", "keep": true},
                {"name": "battle", "description": "", "keep": false},
                {"name": "Fantasy", "description": "", "keep": true},
                {"name": "Stealth", "description": "", "keep": true}
            ]
            """;

        when(gigachatClient.ask(anyString())).thenReturn(responseJson);
        
        Map<String, Object> result1 = new HashMap<>();
        result1.put("name", "RPG");
        result1.put("keep", true);
        
        Map<String, Object> result2 = new HashMap<>();
        result2.put("name", "battle");
        result2.put("keep", false);
        
        Map<String, Object> result3 = new HashMap<>();
        result3.put("name", "Fantasy");
        result3.put("keep", true);
        
        Map<String, Object> result4 = new HashMap<>();
        result4.put("name", "Stealth");
        result4.put("keep", true);
        
        List<Map<String, Object>> results = Arrays.asList(result1, result2, result3, result4);
        
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(results);

        // Act
        List<TagEntity> result = filterTagService.filterTags(testTags);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size()); // 3 tags with keep=true
        assertTrue(result.contains(tag1));
        assertTrue(result.contains(tag3));
        assertTrue(result.contains(tag4));
        
        // Verify tags were saved
        verify(tagRepository, times(4)).save(any(TagEntity.class));
        
        // Verify keep status was updated
        assertTrue(tag1.getKeep());
        assertFalse(tag2.getKeep());
        assertTrue(tag3.getKeep());
        assertTrue(tag4.getKeep());
    }

    @Test
    void filterTags_ShouldRemoveDuplicatesByName() throws Exception {
        // Arrange
        TagEntity duplicateTag = new TagEntity();
        duplicateTag.setId(5L);
        duplicateTag.setName("RPG");
        duplicateTag.setDescription("Another RPG");
        duplicateTag.setKeep(false);
        
        List<TagEntity> tagsWithDuplicates = Arrays.asList(tag1, duplicateTag, tag3);
        
        String responseJson = """
            [
                {"name": "RPG", "description": "", "keep": true},
                {"name": "Fantasy", "description": "", "keep": true}
            ]
            """;
        
        when(gigachatClient.ask(anyString())).thenReturn(responseJson);
        
        Map<String, Object> result1 = new HashMap<>();
        result1.put("name", "RPG");
        result1.put("keep", true);
        
        Map<String, Object> result2 = new HashMap<>();
        result2.put("name", "Fantasy");
        result2.put("keep", true);
        
        List<Map<String, Object>> results = Arrays.asList(result1, result2);
        
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(results);

        // Act
        List<TagEntity> result = filterTagService.filterTags(tagsWithDuplicates);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        // Only one RPG tag should be in filtered results
        long rpgCount = result.stream().filter(t -> t.getName().equals("RPG")).count();
        assertEquals(1, rpgCount);
        
        // Both tags should have been saved
        verify(tagRepository, times(2)).save(any(TagEntity.class));
    }

    @Test
    void filterTags_ShouldProcessInBatchesWhenMoreThanBatchSize() throws Exception {
        // Arrange
        List<TagEntity> largeTagList = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            TagEntity tag = new TagEntity();
            tag.setId((long) i);
            tag.setName("Tag " + i);
            tag.setKeep(false);
            largeTagList.add(tag);
        }
        
        String responseJson = """
            [
                {"name": "Tag 0", "description": "", "keep": true}
            ]
            """;
        
        when(gigachatClient.ask(anyString())).thenReturn(responseJson);
        
        Map<String, Object> result = new HashMap<>();
        result.put("name", "Tag 0");
        result.put("keep", true);
        
        List<Map<String, Object>> results = Collections.singletonList(result);
        
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(results);

        // Act
        List<TagEntity> result1 = filterTagService.filterTags(largeTagList);

        // Assert
        assertNotNull(result1);
        // Should have made multiple calls (at least 3 batches of 50)
        verify(gigachatClient, atLeast(3)).ask(anyString());
    }

    @Test
    void filterTags_ShouldHandleInvalidJsonResponse() throws Exception {
        // Arrange
        String invalidResponseJson = "Some text before [{\"name\": \"RPG\", \"keep\": true}] after";
        
        when(gigachatClient.ask(anyString())).thenReturn(invalidResponseJson);
        
        Map<String, Object> result = new HashMap<>();
        result.put("name", "RPG");
        result.put("keep", true);
        
        List<Map<String, Object>> results = Collections.singletonList(result);
        
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(results);

        // Act
        List<TagEntity> result1 = filterTagService.filterTags(Arrays.asList(tag1));

        // Assert
        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertTrue(tag1.getKeep());
        verify(tagRepository).save(tag1);
    }

    @Test
    void filterTags_ShouldThrowExceptionWhenNoJsonFound() throws Exception {
        // Arrange
        String invalidResponse = "No JSON array here";
        
        when(gigachatClient.ask(anyString())).thenReturn(invalidResponse);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, 
            () -> filterTagService.filterTags(testTags));
        
        assertTrue(exception.getMessage().contains("Не удалось найти JSON в ответе"));
    }

    @Test
    void filterTags_ShouldHandleTagsWithSpecialCharacters() throws Exception {
        // Arrange
        TagEntity specialTag = new TagEntity();
        specialTag.setId(10L);
        specialTag.setName("MMO/RPG");
        specialTag.setDescription("Massively Multiplayer Online");
        specialTag.setKeep(false);
        
        String responseJson = """
            [
                {"name": "MMO/RPG", "description": "", "keep": true}
            ]
            """;
        
        when(gigachatClient.ask(anyString())).thenReturn(responseJson);
        
        Map<String, Object> result = new HashMap<>();
        result.put("name", "MMO/RPG");
        result.put("keep", true);
        
        List<Map<String, Object>> results = Collections.singletonList(result);
        
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(results);

        // Act
        List<TagEntity> result1 = filterTagService.filterTags(Collections.singletonList(specialTag));

        // Assert
        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertTrue(specialTag.getKeep());
        
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(gigachatClient).ask(promptCaptor.capture());
        
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("MMO/RPG"));
    }

    @Test
    void filterTags_ShouldBuildCorrectPrompt() throws Exception {
        // Arrange
        String responseJson = """
            [
                {"name": "RPG", "description": "", "keep": true}
            ]
            """;
        
        when(gigachatClient.ask(anyString())).thenReturn(responseJson);
        
        Map<String, Object> result = new HashMap<>();
        result.put("name", "RPG");
        result.put("keep", true);
        
        List<Map<String, Object>> results = Collections.singletonList(result);
        
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(results);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        filterTagService.filterTags(Collections.singletonList(tag1));

        // Assert
        verify(gigachatClient).ask(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        
        assertTrue(prompt.contains("Ты должен вернуть ТОЛЬКО JSON"));
        assertTrue(prompt.contains("Формат строго:"));
        assertTrue(prompt.contains("{\"name\":\"RPG\",\"description\":\"Role Playing Games\"}"));
        assertTrue(prompt.contains("Оставлять ТОЛЬКО:"));
        assertTrue(prompt.contains("Ставить keep=false если:"));
    }

    @Test
    void filterTags_ShouldHandleNullDescription() throws Exception {
        // Arrange
        TagEntity tagWithNullDesc = new TagEntity();
        tagWithNullDesc.setId(20L);
        tagWithNullDesc.setName("Strategy");
        tagWithNullDesc.setDescription(null);
        tagWithNullDesc.setKeep(false);
        
        String responseJson = """
            [
                {"name": "Strategy", "description": "", "keep": true}
            ]
            """;
        
        when(gigachatClient.ask(anyString())).thenReturn(responseJson);
        
        Map<String, Object> result = new HashMap<>();
        result.put("name", "Strategy");
        result.put("keep", true);
        
        List<Map<String, Object>> results = Collections.singletonList(result);
        
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(results);
        
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        List<TagEntity> result1 = filterTagService.filterTags(Collections.singletonList(tagWithNullDesc));

        // Assert
        assertNotNull(result1);
        assertEquals(1, result1.size());
        
        verify(gigachatClient).ask(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("\"description\":\"\""));
    }

    @Test
    void filterTagsFromDb_ShouldFetchAllTagsAndFilter() throws Exception {
        // Arrange
        when(tagRepository.findAll()).thenReturn(testTags);
        
        String responseJson = """
            [
                {"name": "RPG", "description": "", "keep": true}
            ]
            """;
        
        when(gigachatClient.ask(anyString())).thenReturn(responseJson);
        
        Map<String, Object> result = new HashMap<>();
        result.put("name", "RPG");
        result.put("keep", true);
        
        List<Map<String, Object>> results = Collections.singletonList(result);
        
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(results);

        // Act
        List<TagEntity> result1 = filterTagService.filterTagsFromDb();

        // Assert
        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("RPG", result1.get(0).getName());
        
        verify(tagRepository).findAll();
        verify(tagRepository, atLeast(1)).save(any(TagEntity.class));
    }

    @Test
    void filterTags_ShouldHandleEmptyBatch() throws Exception {
        // Act
        List<TagEntity> result = filterTagService.filterTags(new ArrayList<>());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(gigachatClient, never()).ask(anyString());
        verify(tagRepository, never()).save(any());
    }

    @Test
    void filterTags_ShouldEscapeJsonStringsCorrectly() throws Exception {
        // Arrange
        TagEntity tagWithQuotes = new TagEntity();
        tagWithQuotes.setId(30L);
        tagWithQuotes.setName("Action\"Adventure\"");
        tagWithQuotes.setDescription("Game with \"quotes\"");
        tagWithQuotes.setKeep(false);
        
        String responseJson = """
            [
                {"name": "Action\\"Adventure\\"", "description": "", "keep": true}
            ]
            """;
        
        when(gigachatClient.ask(anyString())).thenReturn(responseJson);
        
        Map<String, Object> result = new HashMap<>();
        result.put("name", "Action\"Adventure\"");
        result.put("keep", true);
        
        List<Map<String, Object>> results = Collections.singletonList(result);
        
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(results);
        
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        List<TagEntity> result1 = filterTagService.filterTags(Collections.singletonList(tagWithQuotes));

        // Assert
        assertNotNull(result1);
        assertTrue(tagWithQuotes.getKeep());
        
        verify(gigachatClient).ask(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Action\\\"Adventure\\\""));
        assertTrue(prompt.contains("Game with \\\"quotes\\\""));
    }

    @Test
    void fixJson_ShouldCleanMalformedJson() throws Exception {
        // This tests the private fixJson method indirectly through filterTags
        
        // Arrange
        String malformedJson = """
            [
                {"name": "RPG", "description": "", "keep": true},
                // This is a comment
                {"name": "Fantasy", "keep": false, "extra": "field"},
                {"name": "Stealth", "keep": true}
            ]
            """;
        
        when(gigachatClient.ask(anyString())).thenReturn(malformedJson);
        
        Map<String, Object> result1 = new HashMap<>();
        result1.put("name", "RPG");
        result1.put("keep", true);
        
        Map<String, Object> result2 = new HashMap<>();
        result2.put("name", "Fantasy");
        result2.put("keep", false);
        
        Map<String, Object> result3 = new HashMap<>();
        result3.put("name", "Stealth");
        result3.put("keep", true);
        
        List<Map<String, Object>> results = Arrays.asList(result1, result2, result3);
        
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(results);

        // Act
        List<TagEntity> filteredTags = filterTagService.filterTags(testTags);

        // Assert
        assertNotNull(filteredTags);
        assertEquals(2, filteredTags.size()); // RPG and Stealth
        assertTrue(tag1.getKeep());
        assertFalse(tag2.getKeep());
        assertTrue(tag4.getKeep());
    }
}