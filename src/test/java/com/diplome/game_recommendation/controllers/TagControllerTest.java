package com.diplome.game_recommendation.controllers;

import com.diplome.game_recommendation.dtos.TagDto;
import com.diplome.game_recommendation.helpers.configuration.Constants;
import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.services.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagControllerTest {

    @Mock
    private TagService tagService;

    @Mock
    private ModelMapper mapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TagController tagController;

    private TagEntity testTag1;
    private TagEntity testTag2;
    private TagEntity testTag3;
    private TagDto testTagDto1;
    private TagDto testTagDto2;
    private TagDto testTagDto3;

    private final Long TEST_TAG_ID_1 = 1L;
    private final Long TEST_TAG_ID_2 = 2L;
    private final Long TEST_TAG_ID_3 = 3L;
    private final String TEST_TAG_NAME_1 = "Action";
    private final String TEST_TAG_NAME_2 = "RPG";
    private final String TEST_TAG_NAME_3 = "Strategy";
    private final String TEST_TAG_NAME_RU_1 = "Экшен";
    private final String TEST_TAG_NAME_RU_2 = "РПГ";
    private final String TEST_TAG_NAME_RU_3 = "Стратегия";
    private final String TEST_SEARCH_QUERY = "act";

    @BeforeEach
    void setUp() {
        // Setup test TagEntity objects
        testTag1 = new TagEntity();
        testTag1.setId(TEST_TAG_ID_1);
        testTag1.setName(TEST_TAG_NAME_1);
        testTag1.setNameRu(TEST_TAG_NAME_RU_1);
        testTag1.setSlug("action");
        testTag1.setKeep(true);

        testTag2 = new TagEntity();
        testTag2.setId(TEST_TAG_ID_2);
        testTag2.setName(TEST_TAG_NAME_2);
        testTag2.setNameRu(TEST_TAG_NAME_RU_2);
        testTag2.setSlug("rpg");
        testTag2.setKeep(true);

        testTag3 = new TagEntity();
        testTag3.setId(TEST_TAG_ID_3);
        testTag3.setName(TEST_TAG_NAME_3);
        testTag3.setNameRu(TEST_TAG_NAME_RU_3);
        testTag3.setSlug("strategy");
        testTag3.setKeep(true);

        // Setup test TagDto objects
        testTagDto1 = new TagDto();
        testTagDto1.setId(TEST_TAG_ID_1);
        testTagDto1.setName(TEST_TAG_NAME_1);
        testTagDto1.setNameRu(TEST_TAG_NAME_RU_1);
        testTagDto1.setSlug("action");

        testTagDto2 = new TagDto();
        testTagDto2.setId(TEST_TAG_ID_2);
        testTagDto2.setName(TEST_TAG_NAME_2);
        testTagDto2.setNameRu(TEST_TAG_NAME_RU_2);
        testTagDto2.setSlug("rpg");

        testTagDto3 = new TagDto();
        testTagDto3.setId(TEST_TAG_ID_3);
        testTagDto3.setName(TEST_TAG_NAME_3);
        testTagDto3.setNameRu(TEST_TAG_NAME_RU_3);
        testTagDto3.setSlug("strategy");
    }

    @Test
    void getAll_ShouldReturnListOfTagDtos() {
        
        int page = 0;
        int size = 20;
        PageRequest pageable = PageRequest.of(page, size);
        List<TagEntity> tags = Arrays.asList(testTag1, testTag2);
        Page<TagEntity> tagPage = new PageImpl<>(tags, pageable, tags.size());

        when(tagService.getAll(page, size)).thenReturn(tagPage);
        when(mapper.map(testTag1, TagDto.class)).thenReturn(testTagDto1);
        when(mapper.map(testTag2, TagDto.class)).thenReturn(testTagDto2);

        
        List<TagDto> result = tagController.getAll(page, size);

        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(TEST_TAG_ID_1, result.get(0).getId());
        assertEquals(TEST_TAG_NAME_1, result.get(0).getName());
        assertEquals(TEST_TAG_NAME_RU_1, result.get(0).getNameRu());
        assertEquals(TEST_TAG_ID_2, result.get(1).getId());
        assertEquals(TEST_TAG_NAME_2, result.get(1).getName());
        assertEquals(TEST_TAG_NAME_RU_2, result.get(1).getNameRu());
        
        verify(tagService).getAll(page, size);
        verify(mapper, times(2)).map(any(TagEntity.class), eq(TagDto.class));
    }

    @Test
    void getAll_ShouldUseDefaultPagination() {
        
        int defaultPage = 0;
        int defaultSize = 20;
        PageRequest pageable = PageRequest.of(defaultPage, defaultSize);
        Page<TagEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(tagService.getAll(defaultPage, defaultSize)).thenReturn(emptyPage);

        
        List<TagDto> result = tagController.getAll(defaultPage, defaultSize);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tagService).getAll(defaultPage, defaultSize);
    }

    @Test
    void getAll_ShouldHandleEmptyResult() {
        
        int page = 0;
        int size = 20;
        PageRequest pageable = PageRequest.of(page, size);
        Page<TagEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(tagService.getAll(page, size)).thenReturn(emptyPage);

        
        List<TagDto> result = tagController.getAll(page, size);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tagService).getAll(page, size);
        verify(mapper, never()).map(any(), any());
    }

    @Test
    void getAll_ShouldSetNameRuCorrectly() {
        
        int page = 0;
        int size = 20;
        PageRequest pageable = PageRequest.of(page, size);
        List<TagEntity> tags = List.of(testTag1);
        Page<TagEntity> tagPage = new PageImpl<>(tags, pageable, tags.size());

        TagDto dtoWithoutNameRu = new TagDto();
        dtoWithoutNameRu.setId(TEST_TAG_ID_1);
        dtoWithoutNameRu.setName(TEST_TAG_NAME_1);
        dtoWithoutNameRu.setNameRu(null);

        when(tagService.getAll(page, size)).thenReturn(tagPage);
        when(mapper.map(testTag1, TagDto.class)).thenReturn(dtoWithoutNameRu);

        
        List<TagDto> result = tagController.getAll(page, size);

        
        assertNotNull(result);
        assertEquals(TEST_TAG_NAME_RU_1, result.get(0).getNameRu());
    }

    @Test
    void get_ShouldReturnTagEntity() {
        
        when(tagService.get(TEST_TAG_ID_1)).thenReturn(testTag1);

        
        TagEntity result = tagController.get(TEST_TAG_ID_1);

        
        assertNotNull(result);
        assertEquals(TEST_TAG_ID_1, result.getId());
        assertEquals(TEST_TAG_NAME_1, result.getName());
        assertEquals(TEST_TAG_NAME_RU_1, result.getNameRu());
        
        verify(tagService).get(TEST_TAG_ID_1);
    }

    @Test
    void get_WhenTagNotFound_ShouldReturnNull() {
        
        when(tagService.get(TEST_TAG_ID_1)).thenReturn(null);

        
        TagEntity result = tagController.get(TEST_TAG_ID_1);

        
        assertNull(result);
        verify(tagService).get(TEST_TAG_ID_1);
    }

    @Test
    void searchTags_WithSearchQuery_ShouldReturnFilteredTags() {
        
        String search = TEST_SEARCH_QUERY;
        int page = 0;
        int size = 5;
        PageRequest pageable = PageRequest.of(page, size);
        List<TagEntity> tags = List.of(testTag1);
        Page<TagEntity> tagPage = new PageImpl<>(tags, pageable, tags.size());

        when(tagService.searchTagsByName(search, page, size)).thenReturn(tagPage);
        when(mapper.map(testTag1, TagDto.class)).thenReturn(testTagDto1);

        
        List<TagDto> result = tagController.searchTags(search, page, size);

        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_TAG_ID_1, result.get(0).getId());
        assertEquals(TEST_TAG_NAME_1, result.get(0).getName());
        
        verify(tagService).searchTagsByName(search, page, size);
        verify(tagService, never()).getAll(anyInt(), anyInt());
    }

    @Test
    void searchTags_WithBlankSearch_ShouldReturnAllTags() {
        
        String search = "";
        int page = 0;
        int size = 5;
        PageRequest pageable = PageRequest.of(page, size);
        List<TagEntity> tags = Arrays.asList(testTag1, testTag2);
        Page<TagEntity> tagPage = new PageImpl<>(tags, pageable, tags.size());

        when(tagService.getAll(page, size)).thenReturn(tagPage);
        when(mapper.map(testTag1, TagDto.class)).thenReturn(testTagDto1);
        when(mapper.map(testTag2, TagDto.class)).thenReturn(testTagDto2);

        
        List<TagDto> result = tagController.searchTags(search, page, size);

        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        verify(tagService).getAll(page, size);
        verify(tagService, never()).searchTagsByName(anyString(), anyInt(), anyInt());
    }

    @Test
    void searchTags_WithWhitespaceSearch_ShouldReturnAllTags() {
        
        String search = "   ";
        int page = 0;
        int size = 5;
        PageRequest pageable = PageRequest.of(page, size);
        List<TagEntity> tags = Arrays.asList(testTag1, testTag2, testTag3);
        Page<TagEntity> tagPage = new PageImpl<>(tags, pageable, tags.size());

        when(tagService.getAll(page, size)).thenReturn(tagPage);
        when(mapper.map(testTag1, TagDto.class)).thenReturn(testTagDto1);
        when(mapper.map(testTag2, TagDto.class)).thenReturn(testTagDto2);
        when(mapper.map(testTag3, TagDto.class)).thenReturn(testTagDto3);

        
        List<TagDto> result = tagController.searchTags(search, page, size);

        
        assertNotNull(result);
        assertEquals(3, result.size());
        
        verify(tagService).getAll(page, size);
        verify(tagService, never()).searchTagsByName(anyString(), anyInt(), anyInt());
    }

    @Test
    void searchTags_ShouldUseDefaultPagination() {
        
        String search = TEST_SEARCH_QUERY;
        int defaultPage = 0;
        int defaultSize = 5;
        PageRequest pageable = PageRequest.of(defaultPage, defaultSize);
        Page<TagEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(tagService.searchTagsByName(search, defaultPage, defaultSize)).thenReturn(emptyPage);

        
        List<TagDto> result = tagController.searchTags(search, defaultPage, defaultSize);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tagService).searchTagsByName(search, defaultPage, defaultSize);
    }

    @Test
    void searchTags_WithNoResults_ShouldReturnEmptyList() {
        
        String search = "nonexistent";
        int page = 0;
        int size = 5;
        PageRequest pageable = PageRequest.of(page, size);
        Page<TagEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(tagService.searchTagsByName(search, page, size)).thenReturn(emptyPage);

        
        List<TagDto> result = tagController.searchTags(search, page, size);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(tagService).searchTagsByName(search, page, size);
    }

    @Test
    void getRecommendedTags_ShouldReturnPageOfTags() {
        
        int page = 0;
        int size = 5;
        PageRequest pageable = PageRequest.of(page, size);
        List<TagEntity> tags = Arrays.asList(testTag1, testTag2);
        Page<TagEntity> tagPage = new PageImpl<>(tags, pageable, tags.size());

        lenient().when(authentication.getName()).thenReturn("test@example.com");
        when(tagService.getTagsSortedByPreference(authentication, page, size)).thenReturn(tagPage);

        
        ResponseEntity<Page<TagEntity>> response = tagController.getRecommendedTags(authentication, page, size);

        
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Page<TagEntity> result = response.getBody();
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(TEST_TAG_ID_1, result.getContent().get(0).getId());
        assertEquals(TEST_TAG_ID_2, result.getContent().get(1).getId());
        
        verify(tagService).getTagsSortedByPreference(authentication, page, size);
    }

    @Test
    void getRecommendedTags_ShouldUseDefaultPagination() {
        
        int defaultPage = 0;
        int defaultSize = 5;
        PageRequest pageable = PageRequest.of(defaultPage, defaultSize);
        Page<TagEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        lenient().when(authentication.getName()).thenReturn("test@example.com");
        when(tagService.getTagsSortedByPreference(authentication, defaultPage, defaultSize))
            .thenReturn(emptyPage);

        
        ResponseEntity<Page<TagEntity>> response = tagController.getRecommendedTags(authentication, defaultPage, defaultSize);

        
        assertNotNull(response);
        Page<TagEntity> result = response.getBody();
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        
        verify(tagService).getTagsSortedByPreference(authentication, defaultPage, defaultSize);
    }

    @Test
    void getRecommendedTags_WhenNoPreferences_ShouldReturnEmptyPage() {
        
        int page = 0;
        int size = 5;
        PageRequest pageable = PageRequest.of(page, size);
        Page<TagEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        lenient().when(authentication.getName()).thenReturn("test@example.com");
        when(tagService.getTagsSortedByPreference(authentication, page, size))
            .thenReturn(emptyPage);

        
        ResponseEntity<Page<TagEntity>> response = tagController.getRecommendedTags(authentication, page, size);

        
        assertNotNull(response);
        Page<TagEntity> result = response.getBody();
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }



    @Test
    void toDto_ShouldSetNameRuCorrectly() {
        
        int page = 0;
        int size = 20;
        PageRequest pageable = PageRequest.of(page, size);
        List<TagEntity> tags = List.of(testTag1);
        Page<TagEntity> tagPage = new PageImpl<>(tags, pageable, tags.size());

        TagDto dtoWithNameRu = new TagDto();
        dtoWithNameRu.setId(TEST_TAG_ID_1);
        dtoWithNameRu.setName(TEST_TAG_NAME_1);
        dtoWithNameRu.setNameRu(TEST_TAG_NAME_RU_1);

        when(tagService.getAll(page, size)).thenReturn(tagPage);
        when(mapper.map(testTag1, TagDto.class)).thenReturn(dtoWithNameRu);

        
        List<TagDto> result = tagController.getAll(page, size);

        
        assertNotNull(result);
        assertEquals(TEST_TAG_NAME_RU_1, result.get(0).getNameRu());
    }

    @Test
    void urlConstant_ShouldBeCorrect() {
        assertEquals("/api/tags", Constants.API_URL + "/tags");
    }

    @Test
    void getAll_ShouldHandleLargePageSize() {
        
        int page = 0;
        int size = 1000;
        PageRequest pageable = PageRequest.of(page, size);
        
        List<TagEntity> manyTags = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            TagEntity tag = new TagEntity();
            tag.setId((long) i);
            manyTags.add(tag);
        }
        Page<TagEntity> tagPage = new PageImpl<>(manyTags, pageable, manyTags.size());

        when(tagService.getAll(page, size)).thenReturn(tagPage);
        
        for (TagEntity tag : manyTags) {
            TagDto dto = new TagDto();
            dto.setId(tag.getId());
            when(mapper.map(tag, TagDto.class)).thenReturn(dto);
        }

        
        List<TagDto> result = tagController.getAll(page, size);

        
        assertNotNull(result);
        assertEquals(size, result.size());
    }

    @Test
    void searchTags_WithEmptySearchAndDefaultParams_ShouldWork() {
        
        String search = "";
        int defaultPage = 0;
        int defaultSize = 5;
        PageRequest pageable = PageRequest.of(defaultPage, defaultSize);
        Page<TagEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(tagService.getAll(defaultPage, defaultSize)).thenReturn(emptyPage);

        
        List<TagDto> result = tagController.searchTags(search, defaultPage, defaultSize);

        
        assertNotNull(result);
        verify(tagService).getAll(defaultPage, defaultSize);
    }
}