package com.diplome.game_recommendation.controllers;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.diplome.game_recommendation.dtos.TagDto;
import com.diplome.game_recommendation.helpers.configuration.*;
import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.services.TagService;

@RestController
@RequestMapping(Constants.API_URL + "/tags")
public class TagController {
    private final TagService tagService;
    private static final Logger logger = LoggerFactory.getLogger(TagController.class);
    private final ModelMapper mapper;

    public TagController(TagService tagService, ModelMapper mapper) {
        this.tagService = tagService;
        this.mapper = mapper;
    }

    @GetMapping
    public Page<TagEntity> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        logger.info("Получение тегов");
        return tagService.getAll(page, size);
    }

    @GetMapping("/{id}")
    public TagEntity get(@PathVariable Long id) {
        logger.info("Получение тега id={}", id);
        return tagService.get(id);
    }

    @GetMapping("/search")
    public List<TagDto> searchTags(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        
        Page<TagEntity> result;
        if (search.isBlank()) {
            result = tagService.getAll(page, size);
        } else {
            result = tagService.searchTagsByName(search, page, size);
        }

        return result.getContent()
                .stream()
                .map(this::toDto)
                .toList();
    }
    private TagDto toDto(TagEntity entity) {
        return mapper.map(entity, TagDto.class);
    }
    @GetMapping("/recommended")
    public ResponseEntity<Page<TagEntity>> getRecommendedTags(
        Authentication authentication, 
        @RequestParam(defaultValue = "0") int page, 
        @RequestParam(defaultValue = "5") int size) {
        
    
    Page<TagEntity> tags = tagService.getTagsSortedByPreference(authentication,page, size);
    return ResponseEntity.ok(tags);
}
}