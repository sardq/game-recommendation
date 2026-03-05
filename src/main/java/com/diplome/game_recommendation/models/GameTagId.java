package com.diplome.game_recommendation.models;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class GameTagId implements Serializable {

    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "tag_id")
    private Long tagId;

    public GameTagId() {}

    public GameTagId(Long gameId, Long tagId) {
        this.gameId = gameId;
        this.tagId = tagId;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}