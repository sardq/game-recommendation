package com.diplome.game_recommendation.dtos.rawg;

import lombok.Data;
import java.util.List;

@Data
public class RawgTagResponse {

    private int count;
    private String next;

    private List<RawgTagDto> results;

}