package com.diplome.game_recommendation.helpers.exceptions;

public class NotFoundException extends RuntimeException {
    public <T> NotFoundException(Class<T> clazz, Long id) {
        super(String.format("%s with id [%s] is not found or not exists", clazz.getSimpleName(), id));
    }
}
