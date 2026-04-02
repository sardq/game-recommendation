package com.diplome.game_recommendation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.repositories.TagRepository;

@Service
public class TagService {
    private static final Logger logger = LoggerFactory.getLogger(TagService.class);
    private static final String LOG_RESPONSE = "Ответ: {}";
    private final TagRepository tagRepository;
    public TagService(TagRepository tagRepository){
        this.tagRepository = tagRepository;
    }
    @Transactional(readOnly = true)
    public Page<TagEntity> getAll(int page, int size) {
        logger.info("Получение тегов: {}, {}", page, size);
        var result = tagRepository.findByKeep(true, PageRequest.of(page, size));
        logger.info(LOG_RESPONSE, result);
        return result;
    }

    @Transactional(readOnly = true)
    public TagEntity get(Long id) {
        logger.info("Получение тега {}", id);
        var result = tagRepository.findById(id).orElse(null);
        logger.info(LOG_RESPONSE, result);
        return result;
    }
    public Page<TagEntity> searchTagsByName(String search, int page, int size) {
    logger.info("Получение тегов по поиску: {}, {}", page, size);
    return tagRepository.filterBySearch(search, PageRequest.of(page, size));
}
}
