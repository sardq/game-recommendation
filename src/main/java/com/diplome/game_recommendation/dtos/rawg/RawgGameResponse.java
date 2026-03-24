package com.diplome.game_recommendation.dtos.rawg;

import lombok.Data;
import java.util.List;

@Data
public class RawgGameResponse {

    private int count;

    private String next;

    private String previous;

    private List<RawgGameDto> results;

}
