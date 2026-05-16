package com.diplome.game_recommendation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.repositories.TagRepository;
import com.diplome.game_recommendation.repositories.UserRepository;

@Service
public class TagService {
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    public TagService(TagRepository tagRepository, UserRepository userRepository){
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }
    @Transactional(readOnly = true)
    public Page<TagEntity> getAll(int page, int size) {
        var result = tagRepository.findByKeep(true, PageRequest.of(page, size));
        return result;
    }

    @Transactional(readOnly = true)
    public TagEntity get(Long id) {
        var result = tagRepository.findById(id).orElse(null);
        return result;
    }
    public Page<TagEntity> searchTagsByName(String search, int page, int size) {
    return tagRepository.filterBySearch(search, PageRequest.of(page, size));
}
@Transactional
public Page<TagEntity> getTagsSortedByPreference( Authentication authentication,  int page, int size) {

    Long userId = userRepository.findByEmail(authentication.getName()).get().getId();
    
    Page<TagEntity> tags = tagRepository.getTagsSortedByPreference(userId, PageRequest.of(page, size));
    return tags;
}
}
