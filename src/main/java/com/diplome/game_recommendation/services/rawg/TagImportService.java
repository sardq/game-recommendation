package com.diplome.game_recommendation.services.rawg;

import org.springframework.stereotype.Service;

import com.diplome.game_recommendation.dtos.rawg.RawgTagDto;
import com.diplome.game_recommendation.dtos.rawg.RawgTagResponse;
import com.diplome.game_recommendation.integration.RawgApiService;
import com.diplome.game_recommendation.models.TagEntity;
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
    public void importImportantTags() throws InterruptedException {
        int page = 1;
        int minGamesCount = 1000; 

        while (true) {
            RawgTagResponse response = rawgApiService.getTags(page);

            if (response.getResults() == null || response.getResults().isEmpty()) break;

            for (RawgTagDto dto : response.getResults()) {
                if (dto.getGames_count() < minGamesCount) continue;
                if (tagRepository.findByName(dto.getName()).isPresent()) continue;
                TagEntity tag = new TagEntity();
                tag.setDescription(dto.getDescription());
                tag.setImageUrl(dto.getImage_background());
                tag.setName(dto.getName());
                tag.setSlug(dto.getSlug());
                tagRepository.save(tag);
            }

            page++;
            if (response.getNext() == null) break;
            Thread.sleep(500);
        }
    }
    public void importTags(int page){

        RawgTagResponse response =
                rawgApiService.getTags(page);

        for(RawgTagDto dto : response.getResults()){

            if(tagRepository.findByName(dto.getName()).isPresent()){
                continue;
            }

            TagEntity tag = new TagEntity();

            tag.setName(dto.getName());
            tag.setImageUrl(dto.getImage_background());
            tag.setDescription(dto.getDescription());
            tag.setSlug(dto.getSlug());
            tagRepository.save(tag);

        }

    }

}