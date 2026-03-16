package com.diplome.game_recommendation.services;

import org.springframework.stereotype.Service;

import com.diplome.game_recommendation.models.TagEntity;
import com.diplome.game_recommendation.dtos.RawgTagDto;
import com.diplome.game_recommendation.dtos.RawgTagResponse;
import com.diplome.game_recommendation.repositories.TagRepository;

@Service
public class TagImportService {

    private final RawgApiService rawgApiService;

    private final TagRepository tagRepository;

    public TagImportService(
            RawgApiService rawgApiService,
            TagRepository tagRepository
    ){
        this.rawgApiService = rawgApiService;
        this.tagRepository = tagRepository;
    }

    public void importTags(){

        RawgTagResponse response =
                rawgApiService.getTags();

        for(RawgTagDto dto : response.getResults()){

            if(tagRepository.findByName(dto.getName()).isPresent()){
                continue;
            }

            TagEntity tag = new TagEntity();

            tag.setName(dto.getName());

            tagRepository.save(tag);

        }

    }

}