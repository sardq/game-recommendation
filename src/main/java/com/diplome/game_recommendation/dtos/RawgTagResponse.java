package com.diplome.game_recommendation.dtos;

import lombok.Data;
import java.util.List;

@Data
public class RawgTagResponse {

    private int count;

    private List<RawgTagDto> results;

}