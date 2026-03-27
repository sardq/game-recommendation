package com.diplome.game_recommendation.controllers;
import com.diplome.game_recommendation.helpers.configuration.*;
import com.diplome.game_recommendation.models.UserPreference;
import com.diplome.game_recommendation.services.UserPreferenceService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.API_URL + "/preferences")
public class UserPreferenceController {

    private final UserPreferenceService service;

    public UserPreferenceController(UserPreferenceService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public List<UserPreference> get(@PathVariable Long userId) {
        return service.getUserPreferences(userId);
    }

    @PostMapping("/refresh/{userId}")
    public void refresh(@PathVariable Long userId) {
        service.updateUserPreferences(userId);
    }
}
