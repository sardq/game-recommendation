package com.diplome.game_recommendation.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tags")
public class TagEntity extends BaseEntity{
    private String name;
    private String description;
    private String imageUrl;
    public TagEntity(){}
    public TagEntity(String name, String description, String imageUrl)
    {
        this.description = description;
        this.name = name;
        this.imageUrl = imageUrl;
    }
    public String getName() {
    return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getImageUrl() {
    return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
