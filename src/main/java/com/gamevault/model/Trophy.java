package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "trophies")
public class Trophy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String iconUrl;

    /** bronze | silver | gold | platinum */
    private String trophyType;

    /** Percentage of PSN users who earned this trophy (e.g. 3.4). Null if unknown. */
    private Double earnedRate;

    /** PSN rarity tier: 0=Ultra Rare, 1=Very Rare, 2=Rare, 3=Common. Null if unknown. */
    private Integer rarity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_game_id", nullable = false)
    private GameCatalog catalogGame;

    public Trophy() {}

    public Long getId()                  { return id; }
    public String getName()              { return name; }
    public String getDescription()       { return description; }
    public String getIconUrl()           { return iconUrl; }
    public String getTrophyType()        { return trophyType; }
    public Double getEarnedRate()        { return earnedRate; }
    public Integer getRarity()           { return rarity; }
    public GameCatalog getCatalogGame()  { return catalogGame; }

    public void setName(String name)               { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setIconUrl(String iconUrl)         { this.iconUrl = iconUrl; }
    public void setTrophyType(String trophyType)   { this.trophyType = trophyType; }
    public void setEarnedRate(Double earnedRate)   { this.earnedRate = earnedRate; }
    public void setRarity(Integer rarity)          { this.rarity = rarity; }
    public void setCatalogGame(GameCatalog catalogGame) { this.catalogGame = catalogGame; }
}
