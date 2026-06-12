package com.gamevault.model;

import jakarta.persistence.*;

/** A manually-registered DLC entry for a catalog game (used for games without a linked Steam App ID). */
@Entity
@Table(name = "catalog_dlcs")
public class CatalogDlc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Double price;

    @Column(columnDefinition = "TEXT")
    private String storeUrl;

    @Column(columnDefinition = "TEXT")
    private String coverUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_game_id", nullable = false)
    private GameCatalog catalogGame;

    public CatalogDlc() {}

    public Long getId()                 { return id; }
    public String getName()             { return name; }
    public Double getPrice()            { return price; }
    public String getStoreUrl()         { return storeUrl; }
    public String getCoverUrl()         { return coverUrl; }
    public GameCatalog getCatalogGame() { return catalogGame; }

    public void setName(String name)               { this.name = name; }
    public void setPrice(Double price)             { this.price = price; }
    public void setStoreUrl(String storeUrl)       { this.storeUrl = storeUrl; }
    public void setCoverUrl(String coverUrl)       { this.coverUrl = coverUrl; }
    public void setCatalogGame(GameCatalog catalogGame) { this.catalogGame = catalogGame; }
}
