package com.diplome.game_recommendation.dtos.rawg;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RawgScreenshotsResponse {
    private List<ScreenshotItem> results;

    @Getter @Setter
    public static class ScreenshotItem {
        private String image;
    }
}