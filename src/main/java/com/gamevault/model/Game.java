package com.gamevault.model;

import com.gamevault.util.RandomIdGenerator;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @GenericGenerator(name = "random_game_id", type = RandomIdGenerator.class,
            parameters = @org.hibernate.annotations.Parameter(name = "table", value = "games"))
    @GeneratedValue(generator = "random_game_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_game_id", nullable = false)
    private GameCatalog catalogGame;

    @Column(nullable = false)
    private String status;

    private Double hours;
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** A personal review/opinion about the game — distinct from private notes, meant to summarize your take on it. */
    @Column(columnDefinition = "TEXT")
    private String review;

    /** Platform the review refers to (e.g. "PS5") — one of the catalog game's platforms. */
    private String reviewPlatform;

    private Integer progress;

    @Column(nullable = false)
    private boolean favorite = false;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Playthrough> playthroughs = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "game_tags", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UnlockedTrophy> unlockedTrophies = new ArrayList<>();

    @Column(nullable = false)
    private Long addedAt;

    /** Timestamp of the last time `status` was changed. */
    private Long statusUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    public Game() {}

    public Long getId()               { return id; }
    public GameCatalog getCatalogGame() { return catalogGame; }
    public String getStatus()         { return status; }
    public Double getHours()          { return hours; }
    public Integer getRating()        { return rating; }
    public String getNotes()          { return notes; }
    public String getReview()         { return review; }
    public String getReviewPlatform() { return reviewPlatform; }
    public Integer getProgress()      { return progress; }
    public boolean isFavorite()       { return favorite; }
    public List<Playthrough> getPlaythroughs() { return playthroughs; }
    public List<String> getTags()     { return tags; }
    public List<UnlockedTrophy> getUnlockedTrophies() { return unlockedTrophies; }
    public Long getAddedAt()          { return addedAt; }
    public Long getStatusUpdatedAt()  { return statusUpdatedAt; }
    public User getOwner()            { return owner; }

    public void setCatalogGame(GameCatalog catalogGame) { this.catalogGame = catalogGame; }
    public void setStatus(String status)           { this.status = status; }
    public void setHours(Double hours)             { this.hours = hours; }
    public void setRating(Integer rating)          { this.rating = rating; }
    public void setNotes(String notes)             { this.notes = notes; }
    public void setReview(String review)           { this.review = review; }
    public void setReviewPlatform(String reviewPlatform) { this.reviewPlatform = reviewPlatform; }
    public void setProgress(Integer progress)      { this.progress = progress; }
    public void setFavorite(boolean favorite)      { this.favorite = favorite; }
    public void setPlaythroughs(List<Playthrough> playthroughs) { this.playthroughs = playthroughs; }
    public void setTags(List<String> tags)         { this.tags = tags; }
    public void setAddedAt(Long addedAt)           { this.addedAt = addedAt; }
    public void setStatusUpdatedAt(Long statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }
    public void setOwner(User owner)               { this.owner = owner; }
}
