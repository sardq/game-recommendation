package com.diplome.game_recommendation.dtos;

import lombok.Data;
import java.util.*;
@Data
public class PublicUserDto {
    private String username;
    private String avatarUrl;
    private List<ReviewDto> reviews; 
}
