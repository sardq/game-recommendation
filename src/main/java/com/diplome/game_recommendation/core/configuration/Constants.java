package com.diplome.game_recommendation.core.configuration;

public class Constants {
    public static final String API_URL = "/api";
    public static final String SEQUENCE_NAME = "hibernate_sequence";
    private static final int MAX_GAMES_FOR_ANALYSIS = 2000;
    private static final int MAX_RECOMMENDATIONS = 20;
    private static final int MIN_INTERACTIONS = 3;
    public static final int DEFUALT_PAGE_SIZE = 5;

    public static final String REDIRECT_VIEW = "redirect:";


    public static final String LOGIN_URL = "/login";
    public static final String LOGOUT_URL = "/logout";


    private Constants() {
    }
}
