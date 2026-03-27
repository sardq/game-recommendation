package com.diplome.game_recommendation.helpers.api;

import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalController {
    @ModelAttribute("servletPath")
    public String getRequestServletPath(HttpServletRequest request) {
        return request.getServletPath();
    }

}