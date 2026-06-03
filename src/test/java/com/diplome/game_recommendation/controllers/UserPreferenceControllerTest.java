package com.diplome.game_recommendation.controllers;

import com.diplome.game_recommendation.dtos.TagPreferenceDto;
import com.diplome.game_recommendation.dtos.UserPreferenceDto;
import com.diplome.game_recommendation.helpers.configuration.Constants;
import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.models.UserPreference;
import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.services.UserPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceControllerTest {

    @Mock
    private UserPreferenceService userPreferenceService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserPreferenceController userPreferenceController;

    private UserPreference testPreference1;
    private UserPreference testPreference2;
    private UserPreferenceDto testPreferenceDto1;
    private UserPreferenceDto testPreferenceDto2;
    private TagPreferenceDto testTagPreferenceDto1;
    private TagPreferenceDto testTagPreferenceDto2;
    private UserEntity testUser;
    private TagEntity testTag1;
    private TagEntity testTag2;

    private final Long TEST_USER_ID = 1L;
    private final Long TEST_TAG_ID_1 = 10L;
    private final Long TEST_TAG_ID_2 = 11L;
    private final String TEST_TAG_NAME_1 = "Action";
    private final String TEST_TAG_NAME_2 = "RPG";
    private final String TEST_TAG_NAME_RU_1 = "Экшен";
    private final String TEST_TAG_NAME_RU_2 = "РПГ";
    private final String TEST_EMAIL = "test@example.com";
    private final Double TEST_WEIGHT_1 = 0.85;
    private final Double TEST_WEIGHT_2 = 0.75;

    @BeforeEach
    void setUp() {
        // Setup test UserEntity
        testUser = new UserEntity();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail(TEST_EMAIL);
        testUser.setUsername("testuser");

        // Setup test TagEntity objects
        testTag1 = new TagEntity();
        testTag1.setId(TEST_TAG_ID_1);
        testTag1.setName(TEST_TAG_NAME_1);
        testTag1.setNameRu(TEST_TAG_NAME_RU_1);
        testTag1.setSlug("action");

        testTag2 = new TagEntity();
        testTag2.setId(TEST_TAG_ID_2);
        testTag2.setName(TEST_TAG_NAME_2);
        testTag2.setNameRu(TEST_TAG_NAME_RU_2);
        testTag2.setSlug("rpg");

        // Setup test UserPreference objects
        testPreference1 = new UserPreference();
        testPreference1.setUser(testUser);
        testPreference1.setTag(testTag1);
        testPreference1.setPreferenceWeight(TEST_WEIGHT_1);

        testPreference2 = new UserPreference();
        testPreference2.setUser(testUser);
        testPreference2.setTag(testTag2);
        testPreference2.setPreferenceWeight(TEST_WEIGHT_2);

        // Setup test UserPreferenceDto objects
        testPreferenceDto1 = new UserPreferenceDto();
        testPreferenceDto1.setTagId(TEST_TAG_ID_1);
        testPreferenceDto1.setTagName(TEST_TAG_NAME_1);
        testPreferenceDto1.setTagNameRu(TEST_TAG_NAME_RU_1);
        testPreferenceDto1.setPreferenceWeight(TEST_WEIGHT_1);

        testPreferenceDto2 = new UserPreferenceDto();
        testPreferenceDto2.setTagId(TEST_TAG_ID_2);
        testPreferenceDto2.setTagName(TEST_TAG_NAME_2);
        testPreferenceDto2.setTagNameRu(TEST_TAG_NAME_RU_2);
        testPreferenceDto2.setPreferenceWeight(TEST_WEIGHT_2);

        // Setup test TagPreferenceDto objects
        testTagPreferenceDto1 = new TagPreferenceDto();
        testTagPreferenceDto1.tagId = TEST_TAG_ID_1;
        testTagPreferenceDto1.rating = 5.0;

        testTagPreferenceDto2 = new TagPreferenceDto();
        testTagPreferenceDto2.tagId = TEST_TAG_ID_2;
        testTagPreferenceDto2.rating = 3.0;
    }

    @Test
    void get_ShouldReturnListOfUserPreferenceDtos() {
        
        List<UserPreference> preferences = Arrays.asList(testPreference1, testPreference2);
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userPreferenceService.getUserPreferences(authentication)).thenReturn(preferences);

        
        List<UserPreferenceDto> result = userPreferenceController.get(authentication);

        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        UserPreferenceDto dto1 = result.get(0);
        assertEquals(TEST_TAG_ID_1, dto1.getTagId());
        assertEquals(TEST_TAG_NAME_1, dto1.getTagName());
        assertEquals(TEST_TAG_NAME_RU_1, dto1.getTagNameRu());
        assertEquals(TEST_WEIGHT_1, dto1.getPreferenceWeight());
        
        UserPreferenceDto dto2 = result.get(1);
        assertEquals(TEST_TAG_ID_2, dto2.getTagId());
        assertEquals(TEST_TAG_NAME_2, dto2.getTagName());
        assertEquals(TEST_TAG_NAME_RU_2, dto2.getTagNameRu());
        assertEquals(TEST_WEIGHT_2, dto2.getPreferenceWeight());
        
        verify(userPreferenceService).getUserPreferences(authentication);
    }

    @Test
    void get_WhenNoPreferences_ShouldReturnEmptyList() {
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userPreferenceService.getUserPreferences(authentication)).thenReturn(List.of());

        
        List<UserPreferenceDto> result = userPreferenceController.get(authentication);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userPreferenceService).getUserPreferences(authentication);
    }


    @Test
    void refresh_ShouldUpdateUserPreferences() {
        
        doNothing().when(userPreferenceService).updateUserPreferences(TEST_USER_ID);

        
        userPreferenceController.refresh(TEST_USER_ID);

        
        verify(userPreferenceService).updateUserPreferences(TEST_USER_ID);
    }

    @Test
    void refresh_WithInvalidUserId_ShouldPropagateException() {
        
        Long invalidUserId = -1L;
        doThrow(new RuntimeException("User not found"))
            .when(userPreferenceService).updateUserPreferences(invalidUserId);

        assertThrows(RuntimeException.class, 
            () -> userPreferenceController.refresh(invalidUserId));
        
        verify(userPreferenceService).updateUserPreferences(invalidUserId);
    }

    @Test
    void refresh_WithNonExistentUserId_ShouldThrowException() {
        
        Long nonExistentUserId = 999L;
        doThrow(new RuntimeException("User not found"))
            .when(userPreferenceService).updateUserPreferences(nonExistentUserId);

        assertThrows(RuntimeException.class, 
            () -> userPreferenceController.refresh(nonExistentUserId));
        
        verify(userPreferenceService).updateUserPreferences(nonExistentUserId);
    }

    @Test
    void initPreferences_ShouldInitializeColdStartPreferences() {
        
        List<TagPreferenceDto> tags = Arrays.asList(testTagPreferenceDto1, testTagPreferenceDto2);
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        doNothing().when(userPreferenceService)
            .initializeColdStartPreferencesWithRating(authentication, tags);

        
        userPreferenceController.initPreferences(tags, authentication);

        
        verify(userPreferenceService).initializeColdStartPreferencesWithRating(authentication, tags);
    }

    @Test
    void initPreferences_WithEmptyList_ShouldStillCallService() {
        
        List<TagPreferenceDto> emptyTags = List.of();
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        doNothing().when(userPreferenceService)
            .initializeColdStartPreferencesWithRating(authentication, emptyTags);

        
        userPreferenceController.initPreferences(emptyTags, authentication);

        
        verify(userPreferenceService).initializeColdStartPreferencesWithRating(authentication, emptyTags);
    }

    @Test
    void initPreferences_WithSingleTag_ShouldInitialize() {
        
        List<TagPreferenceDto> singleTag = List.of(testTagPreferenceDto1);
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        doNothing().when(userPreferenceService)
            .initializeColdStartPreferencesWithRating(authentication, singleTag);

        
        userPreferenceController.initPreferences(singleTag, authentication);

        
        verify(userPreferenceService).initializeColdStartPreferencesWithRating(authentication, singleTag);
    }


    @Test
    void hasPreferences_WhenPreferencesExist_ShouldReturnTrue() {
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userPreferenceService.countByUserId(authentication)).thenReturn(5L);

        
        ResponseEntity<Boolean> response = userPreferenceController.hasPreferences(authentication);

        
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Boolean result = response.getBody();
        assertNotNull(result);
        assertTrue(result);
        
        verify(userPreferenceService).countByUserId(authentication);
    }

    @Test
    void hasPreferences_WhenNoPreferences_ShouldReturnFalse() {
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userPreferenceService.countByUserId(authentication)).thenReturn(0L);

        
        ResponseEntity<Boolean> response = userPreferenceController.hasPreferences(authentication);

        
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Boolean result = response.getBody();
        assertNotNull(result);
        assertFalse(result);
        
        verify(userPreferenceService).countByUserId(authentication);
    }


    @Test
    void toDto_ShouldMapUserPreferenceCorrectly() {
        // This tests the private toDto method indirectly through get
        
        List<UserPreference> preferences = List.of(testPreference1);
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userPreferenceService.getUserPreferences(authentication)).thenReturn(preferences);

        
        List<UserPreferenceDto> result = userPreferenceController.get(authentication);

        
        assertNotNull(result);
        assertEquals(1, result.size());
        
        UserPreferenceDto dto = result.get(0);
        assertEquals(TEST_TAG_ID_1, dto.getTagId());
        assertEquals(TEST_TAG_NAME_1, dto.getTagName());
        assertEquals(TEST_TAG_NAME_RU_1, dto.getTagNameRu());
        assertEquals(TEST_WEIGHT_1, dto.getPreferenceWeight());
    }

    @Test
    void toDto_ShouldHandleNullValues() {
        // This tests the private toDto method with null values
        
        TagEntity tagWithNulls = new TagEntity();
        tagWithNulls.setId(TEST_TAG_ID_1);
        tagWithNulls.setName(null);
        tagWithNulls.setNameRu(null);
        
        UserPreference preferenceWithNulls = new UserPreference();
        preferenceWithNulls.setTag(tagWithNulls);
        preferenceWithNulls.setPreferenceWeight(TEST_WEIGHT_1);
        
        List<UserPreference> preferences = List.of(preferenceWithNulls);
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userPreferenceService.getUserPreferences(authentication)).thenReturn(preferences);

        
        List<UserPreferenceDto> result = userPreferenceController.get(authentication);

        
        assertNotNull(result);
        assertEquals(1, result.size());
        
        UserPreferenceDto dto = result.get(0);
        assertEquals(TEST_TAG_ID_1, dto.getTagId());
        assertNull(dto.getTagName());
        assertNull(dto.getTagNameRu());
        assertEquals(TEST_WEIGHT_1, dto.getPreferenceWeight());
    }

    @Test
    void urlConstant_ShouldBeCorrect() {
        assertEquals("/api/preferences", Constants.API_URL + "/preferences");
    }

    @Test
    void get_ShouldHandleMultiplePreferences() {
        
        List<UserPreference> manyPreferences = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            TagEntity tag = new TagEntity();
            tag.setId((long) i);
            tag.setName("Tag " + i);
            tag.setNameRu("Тег " + i);
            
            UserPreference pref = new UserPreference();
            pref.setTag(tag);
            pref.setPreferenceWeight(i / 10.0);
            manyPreferences.add(pref);
        }
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(userPreferenceService.getUserPreferences(authentication)).thenReturn(manyPreferences);

        
        List<UserPreferenceDto> result = userPreferenceController.get(authentication);

        
        assertNotNull(result);
        assertEquals(10, result.size());
        
        for (int i = 0; i < 10; i++) {
            assertEquals(i + 1, result.get(i).getTagId());
            assertEquals("Tag " + (i + 1), result.get(i).getTagName());
            assertEquals("Тег " + (i + 1), result.get(i).getTagNameRu());
            assertEquals((i + 1) / 10.0, result.get(i).getPreferenceWeight());
        }
    }

    @Test
    void initPreferences_ShouldPassCorrectTagsToService() {
        
        List<TagPreferenceDto> tags = Arrays.asList(testTagPreferenceDto1, testTagPreferenceDto2);
        
        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
        doNothing().when(userPreferenceService)
            .initializeColdStartPreferencesWithRating(authentication, tags);

        
        userPreferenceController.initPreferences(tags, authentication);

        
        verify(userPreferenceService).initializeColdStartPreferencesWithRating(authentication, tags);
    }
}