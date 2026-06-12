package com.gamevault.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_catalog")
public class GameCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    private String emoji;

    @Column(columnDefinition = "TEXT")
    private String coverUrl;

    @Column(columnDefinition = "TEXT")
    private String heroImageUrl;
    private Integer heroImagePositionY;
    private Long steamAppId;

    /** PSN NP Communication ID (e.g. NPWR01408_00) — used to fetch PSN trophies */
    private String psnNpCommunicationId;

    /** PSN Store Product ID (e.g. UP9000-PPSA03396_00-THELASTOFUSPART1) — used to fetch the live PS Store price */
    private String psnProductId;
    private String platform;
    private String genre;
    private Long releaseDate;
    private String developer;
    private String publisher;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String difficulties;

    private boolean hasDlc;

    @OneToMany(mappedBy = "catalogGame", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trophy> trophies = new ArrayList<>();

    @OneToMany(mappedBy = "catalogGame", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CatalogDlc> dlcs = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "game_catalog_franchises",
            joinColumns = @JoinColumn(name = "game_catalog_id"),
            inverseJoinColumns = @JoinColumn(name = "franchise_id")
    )
    private List<Franchise> franchises = new ArrayList<>();

    public GameCatalog() {}

    public List<Trophy> getTrophies() { return trophies; }
    public List<CatalogDlc> getDlcs() { return dlcs; }
    public List<Franchise> getFranchises() { return franchises; }
    public void setFranchises(List<Franchise> franchises) { this.franchises = franchises; }

    public Long getId()            { return id; }
    public String getTitle()       { return title; }
    public String getEmoji()       { return emoji; }
    public String getCoverUrl()    { return coverUrl; }
    public String getHeroImageUrl() { return heroImageUrl; }
    public Integer getHeroImagePositionY() { return heroImagePositionY; }
    public Long getSteamAppId() { return steamAppId; }
    public String getPsnNpCommunicationId() { return psnNpCommunicationId; }
    public String getPsnProductId() { return psnProductId; }
    public String getPlatform()    { return platform; }
    public String getGenre()       { return genre; }
    public Long getReleaseDate()   { return releaseDate; }
    public String getDeveloper()   { return developer; }
    public String getPublisher()   { return publisher; }
    public String getDescription() { return description; }
    public String getDifficulties() { return difficulties; }
    public boolean isHasDlc()      { return hasDlc; }

    public void setTitle(String title)             { this.title = title; }
    public void setEmoji(String emoji)             { this.emoji = emoji; }
    public void setCoverUrl(String coverUrl)       { this.coverUrl = coverUrl; }
    public void setHeroImageUrl(String heroImageUrl) { this.heroImageUrl = heroImageUrl; }
    public void setHeroImagePositionY(Integer heroImagePositionY) { this.heroImagePositionY = heroImagePositionY; }
    public void setSteamAppId(Long steamAppId) { this.steamAppId = steamAppId; }
    public void setPsnNpCommunicationId(String id) { this.psnNpCommunicationId = id; }
    public void setPsnProductId(String id) { this.psnProductId = id; }
    public void setPlatform(String platform)       { this.platform = platform; }
    public void setGenre(String genre)             { this.genre = genre; }
    public void setReleaseDate(Long releaseDate)   { this.releaseDate = releaseDate; }
    public void setDeveloper(String developer)     { this.developer = developer; }
    public void setPublisher(String publisher)     { this.publisher = publisher; }
    public void setDescription(String description) { this.description = description; }
    public void setDifficulties(String difficulties) { this.difficulties = difficulties; }
    public void setHasDlc(boolean hasDlc)             { this.hasDlc = hasDlc; }
}
