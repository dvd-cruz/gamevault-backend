package com.gamevault.model;

import com.gamevault.util.RandomIdGenerator;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @GenericGenerator(name = "random_activity_id", type = RandomIdGenerator.class,
            parameters = @org.hibernate.annotations.Parameter(name = "table", value = "activities"))
    @GeneratedValue(generator = "random_activity_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(nullable = false)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_game_id", nullable = false)
    private GameCatalog catalogGame;

    private Integer count;
    private Integer rating;
    private Double hours;
    private Long trophyId;

    /** Platform the review refers to (e.g. "PS5") — only set on "review" activities. */
    private String platform;

    @Column(columnDefinition = "TEXT")
    private String message;

    /** Optional image attached to a "post" activity (data URL or external URL). */
    @Column(columnDefinition = "TEXT")
    private String image;

    @Column(nullable = false)
    private Long createdAt;

    /** Set when a review is edited in-place — null for original/unedited activities. */
    private Long editedAt;

    public Activity() {}

    public Long getId()                 { return id; }
    public User getActor()              { return actor; }
    public String getType()             { return type; }
    public GameCatalog getCatalogGame() { return catalogGame; }
    public Integer getCount()           { return count; }
    public Integer getRating()          { return rating; }
    public Double getHours()            { return hours; }
    public String getMessage()          { return message; }
    public String getImage()            { return image; }
    public Long getCreatedAt()          { return createdAt; }
    public Long getTrophyId()           { return trophyId; }
    public Long getEditedAt()           { return editedAt; }
    public String getPlatform()         { return platform; }

    public void setActor(User actor)               { this.actor = actor; }
    public void setType(String type)               { this.type = type; }
    public void setCatalogGame(GameCatalog catalogGame) { this.catalogGame = catalogGame; }
    public void setCount(Integer count)            { this.count = count; }
    public void setRating(Integer rating)          { this.rating = rating; }
    public void setHours(Double hours)             { this.hours = hours; }
    public void setMessage(String message)         { this.message = message; }
    public void setImage(String image)             { this.image = image; }
    public void setCreatedAt(Long createdAt)       { this.createdAt = createdAt; }
    public void setTrophyId(Long trophyId)         { this.trophyId = trophyId; }
    public void setEditedAt(Long editedAt)         { this.editedAt = editedAt; }
    public void setPlatform(String platform)       { this.platform = platform; }
}
