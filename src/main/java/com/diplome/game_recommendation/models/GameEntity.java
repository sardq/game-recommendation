package com.diplome.game_recommendation.models;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
@Entity
@Table(name = "games")
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
    private String developers;
    private String publishers;
    private BigDecimal metacriticRate;
    private Integer playtime ;
    private BigDecimal rating;
    public GameEntity(){
    }
     public GameEntity(String name, String description, Date releaseDate, 
                      Set<PlatformEnum> platforms, String posterUrl, BigDecimal rating) {
        this.name = name;
        this.description = description;
        this.releaseDate = releaseDate;
        this.platforms = platforms;
        this.posterUrl = posterUrl;
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getRawgId() {
        return rawgId;
    }

    public void setRawgId(Long rawgId) {
        this.rawgId = rawgId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Set<PlatformEnum> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(Set<PlatformEnum> platforms) {
        this.platforms = platforms;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getDevelopers() {
        return developers;
    }

    public void setDevelopers(String developers) {
        this.developers = developers;
    }

    public String getPublishers() {
        return publishers;
    }

    public void setPublishers(String publishers) {
        this.publishers = publishers;
    }

    public BigDecimal getMetacriticRate() {
        return metacriticRate;
    }

    public void setMetacriticRate(BigDecimal metacritic_rate) {
        this.metacriticRate = metacritic_rate;
    }

    public Integer getPlaytime() {
        return playtime;
    }

    public void setPlaytime(Integer playtime) {
        this.playtime = playtime;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
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
                ", developers='" + developers + '\'' +
                ", publishers='" + publishers + '\'' +
                ", metacritic_rate=" + metacriticRate +
                ", playtime=" + playtime +
                ", rating=" + rating +
                '}';
    }
}
