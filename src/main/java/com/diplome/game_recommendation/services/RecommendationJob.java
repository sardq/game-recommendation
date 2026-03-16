package com.diplome.game_recommendation.services;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.repositories.UserRepository;

public class RecommendationJob implements Job {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserPreferenceService preferenceService;
    @Override
    public void execute(JobExecutionContext context){
        List<UserEntity> users = userRepository.findAll();
        for(UserEntity user : users){
            preferenceService.updateUserPreferences(user.getId());
        }
    }
}
