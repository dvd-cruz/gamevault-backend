package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tier_list_entries")
public class TierListEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tier row this game sits in: S, A, B, C or D. */
    @Column(nullable = false)
    private String tier;

    /** Order within the tier row. */
    @Column(nullable = false)
    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_game_id", nullable = false)
    private GameCatalog catalogGame;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_list_id", nullable = false)
    private TierList tierList;

    public TierListEntry() {}

    public Long getId()              { return id; }
    public String getTier()          { return tier; }
    public Integer getPosition()     { return position; }
    public GameCatalog getCatalogGame() { return catalogGame; }
    public TierList getTierList()    { return tierList; }

    public void setTier(String tier)                 { this.tier = tier; }
    public void setPosition(Integer position)        { this.position = position; }
    public void setCatalogGame(GameCatalog catalogGame) { this.catalogGame = catalogGame; }
    public void setTierList(TierList tierList)       { this.tierList = tierList; }
}
