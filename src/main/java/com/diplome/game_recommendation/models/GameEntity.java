package com.diplome.game_recommendation.models;

import java.util.Date;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name = "games")
@Getter @Setter
public class GameEntity extends BaseEntity {
    private String name;
    private Long rawgId;
    private String description;
    private Date releaseDate;
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "game_platforms", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "platform")
    private Set<PlatformEnum> platforms;
    private String posterUrl;
    private Double metacriticRate;
    private Integer playtime;
    private Double rating;
    @Column(name = "local_rating")
    private Double localRating = 0.0;
    @Column(name = "local_rating_count")
    private Integer localRatingCount = 0;
    @OneToMany(mappedBy = "game", fetch = FetchType.LAZY)
    private Set<GameTag> gameTags;
    public GameEntity(){
    }
     public GameEntity(String name, String description, Date releaseDate, 
                      Set<PlatformEnum> platforms, String posterUrl, Double rating) {
        this.name = name;
        this.description = description;
        this.releaseDate = releaseDate;
        this.platforms = platforms;
        this.posterUrl = posterUrl;
        this.rating = rating;
    }


    @Override
    public String toString() {
        return "GameEntity{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", rawgId=" + rawgId +
                ", description='" + description + '\'' +
                ", releaseDate=" + releaseDate +
                ", posterUrl='" + posterUrl + '\'' +
                ", metacritic_rate=" + metacriticRate +
                ", playtime=" + playtime +
                ", rating=" + rating +
                '}';
    }
}
