package com.diplome.game_recommendation.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import com.diplome.game_recommendation.services.GameImportService;
import com.diplome.game_recommendation.services.TagImportService;

public class RawgUpdateJob implements Job {

    @Autowired
    private GameImportService gameImportService;

    @Autowired
    private TagImportService tagImportService;

    @Override
    public void execute(JobExecutionContext context){

        tagImportService.importTags();

        for(int i=1;i<=40;i++){
            gameImportService.importGames(i);
        }

    }

}