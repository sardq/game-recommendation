package com.diplome.game_recommendation.core.configuration;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import com.diplome.game_recommendation.services.rawg.GameImportService;

public class RawgUpdateJob implements Job {

    @Autowired
    private GameImportService gameImportService;


    @Override
    public void execute(JobExecutionContext context){


        for(int i=1;i<=40;i++){
            gameImportService.importGames(i);
        }

    }

}