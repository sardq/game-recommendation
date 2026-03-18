package com.diplome.game_recommendation.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.diplome.game_recommendation.core.configuration.*;
import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.services.TagService;

@RestController
@RequestMapping(Constants.API_URL + "/tags")
public class TagController {
    private final TagService tagService;
    private static final Logger logger = LoggerFactory.getLogger(TagController.class);

    public TagController(TagService tagService) {
        this.tagService = tagService;
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
}