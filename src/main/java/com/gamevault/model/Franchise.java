package com.gamevault.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** Groups multiple catalog games together (e.g. "Sonic" groups Sonic Generations, Sonic Forces, Sonic Frontiers). */
@Entity
@Table(name = "franchises")
public class Franchise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** Emoji or image URL shown when users showcase this franchise as a favourite. */
    @Column(columnDefinition = "TEXT")
    private String iconUrl;

    @ManyToMany(mappedBy = "franchises")
    private List<GameCatalog> games = new ArrayList<>();

    public Franchise() {}

    public Long getId()      { return id; }
    public String getName()  { return name; }
    public String getIconUrl() { return iconUrl; }
    public List<GameCatalog> getGames() { return games; }

    public void setName(String name)       { this.name = name; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
}
