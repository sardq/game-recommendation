package com.diplome.game_recommendation.services;


import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.repositories.TagRepository;
import com.diplome.game_recommendation.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TagService tagService;

    private TagEntity testTag1;
    private TagEntity testTag2;
    private TagEntity testTag3;
    
    private final Long TEST_TAG_ID_1 = 1L;
    private final Long TEST_TAG_ID_2 = 2L;
    private final Long TEST_TAG_ID_3 = 3L;
    private final Long TEST_USER_ID = 100L;
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        testTag1 = new TagEntity();
        testTag1.setId(TEST_TAG_ID_1);
        testTag1.setName("Action");
        testTag1.setKeep(true);
        testTag1.setDescription("Action games tag");

        testTag2 = new TagEntity();
        testTag2.setId(TEST_TAG_ID_2);
        testTag2.setName("RPG");
        testTag2.setKeep(true);
        testTag2.setDescription("Role Playing Games tag");

        testTag3 = new TagEntity();
        testTag3.setId(TEST_TAG_ID_3);
        testTag3.setName("Strategy");
        testTag3.setKeep(false); // This tag is not kept
        testTag3.setDescription("Strategy games tag");

        lenient().when(authentication.getName()).thenReturn(TEST_EMAIL);
    }

    @Test
    void getAll_ShouldReturnPageOfTags_WithKeepTrue() {
        
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<TagEntity> tags = Arrays.asList(testTag1, testTag2);
        Page<TagEntity> expectedPage = new PageImpl<>(tags, pageable, tags.size());

        when(tagRepository.findByKeep(eq(true), eq(pageable))).thenReturn(expectedPage);

        
        Page<TagEntity> result = tagService.getAll(page, size);

        
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(testTag1, result.getContent().get(0));
        assertEquals(testTag2, result.getContent().get(1));
        verify(tagRepository).findByKeep(true, pageable);
    }

    @Test
    void getAll_ShouldReturnEmptyPage_WhenNoTags() {
        
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<TagEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(tagRepository.findByKeep(eq(true), eq(pageable))).thenReturn(emptyPage);

        
        Page<TagEntity> result = tagService.getAll(page, size);

        
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(tagRepository).findByKeep(true, pageable);
    }

    @Test
    void getAll_ShouldHandleDifferentPageSizes() {
        
        int page = 2;
        int size = 5;
        Pageable pageable = PageRequest.of(page, size);
        List<TagEntity> tags = Arrays.asList(testTag1, testTag2);
        Page<TagEntity> expectedPage = new PageImpl<>(tags, pageable, 20);

        when(tagRepository.findByKeep(eq(true), eq(pageable))).thenReturn(expectedPage);

        
        Page<TagEntity> result = tagService.getAll(page, size);

        
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(20, result.getTotalElements());
        assertEquals(page, result.getPageable().getPageNumber());
        assertEquals(size, result.getPageable().getPageSize());
        verify(tagRepository).findByKeep(true, pageable);
    }

    @Test
    void get_WhenTagExists_ShouldReturnTag() {
        
        when(tagRepository.findById(TEST_TAG_ID_1)).thenReturn(Optional.of(testTag1));

        
        TagEntity result = tagService.get(TEST_TAG_ID_1);

        
        assertNotNull(result);
        assertEquals(TEST_TAG_ID_1, result.getId());
        assertEquals("Action", result.getName());
        assertTrue(result.getKeep());
        verify(tagRepository).findById(TEST_TAG_ID_1);
    }

    @Test
    void get_WhenTagDoesNotExist_ShouldReturnNull() {
        
        Long nonExistentId = 999L;
        when(tagRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        
        TagEntity result = tagService.get(nonExistentId);

        
        assertNull(result);
        verify(tagRepository).findById(nonExistentId);
    }

    @Test
    void get_ShouldHandleTagWithKeepFalse() {
        
        when(tagRepository.findById(TEST_TAG_ID_3)).thenReturn(Optional.of(testTag3));

        
        TagEntity result = tagService.get(TEST_TAG_ID_3);

        
        assertNotNull(result);
        assertEquals(TEST_TAG_ID_3, result.getId());
        assertEquals("Strategy", result.getName());
        assertFalse(result.getKeep()); // Should return even if keep is false
        verify(tagRepository).findById(TEST_TAG_ID_3);
    }

    @Test
    void searchTagsByName_ShouldReturnMatchingTags() {
        
        String searchQuery = "act";
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<TagEntity> matchingTags = Arrays.asList(testTag1);
        Page<TagEntity> expectedPage = new PageImpl<>(matchingTags, pageable, matchingTags.size());

        when(tagRepository.filterBySearch(eq(searchQuery), eq(pageable))).thenReturn(expectedPage);

        
        Page<TagEntity> result = tagService.searchTagsByName(searchQuery, page, size);

        
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Action", result.getContent().get(0).getName());
        verify(tagRepository).filterBySearch(searchQuery, pageable);
    }

    @Test
    void searchTagsByName_WithEmptySearch_ShouldReturnAllTags() {
        
        String searchQuery = "";
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<TagEntity> allTags = Arrays.asList(testTag1, testTag2);
        Page<TagEntity> expectedPage = new PageImpl<>(allTags, pageable, allTags.size());

        when(tagRepository.filterBySearch(eq(searchQuery), eq(pageable))).thenReturn(expectedPage);

        
        Page<TagEntity> result = tagService.searchTagsByName(searchQuery, page, size);

        
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(tagRepository).filterBySearch(searchQuery, pageable);
    }

    @Test
    void searchTagsByName_WithNoMatches_ShouldReturnEmptyPage() {
        
        String searchQuery = "nonexistent";
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<TagEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(tagRepository.filterBySearch(eq(searchQuery), eq(pageable))).thenReturn(emptyPage);

        
        Page<TagEntity> result = tagService.searchTagsByName(searchQuery, page, size);

        
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(tagRepository).filterBySearch(searchQuery, pageable);
    }

    @Test
    void searchTagsByName_ShouldHandlePartialMatches() {
        
        String searchQuery = "R";
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<TagEntity> matchingTags = Arrays.asList(testTag2); // RPG matches
        Page<TagEntity> expectedPage = new PageImpl<>(matchingTags, pageable, matchingTags.size());

        when(tagRepository.filterBySearch(eq(searchQuery), eq(pageable))).thenReturn(expectedPage);

        
        Page<TagEntity> result = tagService.searchTagsByName(searchQuery, page, size);

        
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("RPG", result.getContent().get(0).getName());
        verify(tagRepository).filterBySearch(searchQuery, pageable);
    }

    @Test
    void getTagsSortedByPreference_ShouldReturnTagsSortedByUserPreference() {
        
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        TagEntity preferredTag1 = new TagEntity();
        preferredTag1.setId(10L);
        preferredTag1.setName("Action");
        
        TagEntity preferredTag2 = new TagEntity();
        preferredTag2.setId(11L);
        preferredTag2.setName("Adventure");
        
        List<TagEntity> sortedTags = Arrays.asList(preferredTag1, preferredTag2);
        Page<TagEntity> expectedPage = new PageImpl<>(sortedTags, pageable, sortedTags.size());

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(createUser()));
        when(tagRepository.getTagsSortedByPreference(eq(TEST_USER_ID), eq(pageable))).thenReturn(expectedPage);

        
        Page<TagEntity> result = tagService.getTagsSortedByPreference(authentication, page, size);

        
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(10L, result.getContent().get(0).getId());
        assertEquals(11L, result.getContent().get(1).getId());
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(tagRepository).getTagsSortedByPreference(TEST_USER_ID, pageable);
    }

    @Test
    void getTagsSortedByPreference_ShouldHandleEmptyResult() {
        
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<TagEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(createUser()));
        when(tagRepository.getTagsSortedByPreference(eq(TEST_USER_ID), eq(pageable))).thenReturn(emptyPage);

        
        Page<TagEntity> result = tagService.getTagsSortedByPreference(authentication, page, size);

        
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(tagRepository).getTagsSortedByPreference(TEST_USER_ID, pageable);
    }

    @Test
    void getTagsSortedByPreference_WhenUserNotFound_ShouldThrowException() {
        
        int page = 0;
        int size = 10;
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, 
            () -> tagService.getTagsSortedByPreference(authentication, page, size));
        
        verify(tagRepository, never()).getTagsSortedByPreference(any(), any());
    }

    @Test
    void getAll_ShouldLogCorrectly() {
        
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<TagEntity> expectedPage = new PageImpl<>(List.of(testTag1), pageable, 1);

        when(tagRepository.findByKeep(eq(true), eq(pageable))).thenReturn(expectedPage);

        
        Page<TagEntity> result = tagService.getAll(page, size);

        
        assertNotNull(result);
        verify(tagRepository).findByKeep(true, pageable);
    }

    @Test
    void get_ShouldLogCorrectly() {
        
        when(tagRepository.findById(TEST_TAG_ID_1)).thenReturn(Optional.of(testTag1));

        
        TagEntity result = tagService.get(TEST_TAG_ID_1);

        
        assertNotNull(result);
        verify(tagRepository).findById(TEST_TAG_ID_1);
    }

    @Test
    void searchTagsByName_ShouldHandleNullSearchQuery() {
        
        String searchQuery = null;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        when(tagRepository.filterBySearch(isNull(), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        
        Page<TagEntity> result = tagService.searchTagsByName(searchQuery, page, size);

        
        assertNotNull(result);
        verify(tagRepository).filterBySearch(isNull(), eq(pageable));
    }

    @Test
    void getTagsSortedByPreference_ShouldPreserveOrder() {
        
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        // Create tags in expected preference order
        TagEntity mostPreferred = new TagEntity();
        mostPreferred.setId(1L);
        mostPreferred.setName("Most Preferred");
        
        TagEntity secondPreferred = new TagEntity();
        secondPreferred.setId(2L);
        secondPreferred.setName("Second Preferred");
        
        TagEntity leastPreferred = new TagEntity();
        leastPreferred.setId(3L);
        leastPreferred.setName("Least Preferred");
        
        List<TagEntity> sortedByPreference = Arrays.asList(mostPreferred, secondPreferred, leastPreferred);
        Page<TagEntity> expectedPage = new PageImpl<>(sortedByPreference, pageable, sortedByPreference.size());

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(createUser()));
        when(tagRepository.getTagsSortedByPreference(eq(TEST_USER_ID), eq(pageable))).thenReturn(expectedPage);

        
        Page<TagEntity> result = tagService.getTagsSortedByPreference(authentication, page, size);

        
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals(2L, result.getContent().get(1).getId());
        assertEquals(3L, result.getContent().get(2).getId());
        verify(tagRepository).getTagsSortedByPreference(TEST_USER_ID, pageable);
    }

    @Test
    void getAll_ShouldOnlyReturnTagsWithKeepTrue() {
        
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        List<TagEntity> keptTags = Arrays.asList(testTag1, testTag2);
        Page<TagEntity> expectedPage = new PageImpl<>(keptTags, pageable, keptTags.size());

        when(tagRepository.findByKeep(eq(true), eq(pageable))).thenReturn(expectedPage);

        
        Page<TagEntity> result = tagService.getAll(page, size);

        
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        // Verify that testTag3 (keep=false) is not in the results
        assertFalse(result.getContent().contains(testTag3));
        verify(tagRepository).findByKeep(true, pageable);
    }

    // Helper method to create a UserEntity
    private com.diplome.game_recommendation.models.UserEntity createUser() {
        com.diplome.game_recommendation.models.UserEntity user = 
            new com.diplome.game_recommendation.models.UserEntity();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setUsername("testuser");
        return user;
    }
}