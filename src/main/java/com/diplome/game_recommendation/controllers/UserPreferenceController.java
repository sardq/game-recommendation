package com.diplome.game_recommendation.controllers;
import com.diplome.game_recommendation.dtos.TagPreferenceDto;
import com.diplome.game_recommendation.dtos.UserPreferenceDto;
import com.diplome.game_recommendation.helpers.configuration.*;
import com.diplome.game_recommendation.models.UserPreference;
import com.diplome.game_recommendation.services.UserPreferenceService;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.API_URL + "/preferences")
public class UserPreferenceController {

    private final UserPreferenceService service;
    public UserPreferenceController(UserPreferenceService service) {
        this.service = service;
    }
    private UserPreferenceDto toDto(UserPreference entity) {
           UserPreferenceDto dto = new UserPreferenceDto();
            dto.setPreferenceWeight(entity.getPreferenceWeight().doubleValue());
            dto.setTagId(entity.getTag().getId());      
            dto.setTagName(entity.getTag().getName());
            return dto;
        }

    @GetMapping("/user")
    public List<UserPreferenceDto> get(Authentication authentication) {
        return service.getUserPreferences(authentication).stream().map(this::toDto).toList();
    }

    @PostMapping("/refresh/{userId}")
    public void refresh(@PathVariable Long userId) {
        service.updateUserPreferences(userId);
    }
    @PostMapping("/init")
    public void initPreferences(
            @RequestBody List<TagPreferenceDto> tags,
            Authentication authentication
    ) {
        service.initializeColdStartPreferencesWithRating(authentication, tags);
}
}
