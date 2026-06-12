package com.gamevault.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_lists")
public class GameList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private Long createdAt;

    @OneToMany(mappedBy = "gameList", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<GameListItem> games = new ArrayList<>();

    public GameList() {}

    public Long getId()                   { return id; }
    public String getSlug()               { return slug; }
    public String getTitle()              { return title; }
    public String getDescription()        { return description; }
    public User getAuthor()               { return author; }
    public Long getCreatedAt()            { return createdAt; }
    public List<GameListItem> getGames()  { return games; }

    public void setSlug(String slug)             { this.slug = slug; }
    public void setTitle(String title)           { this.title = title; }
    public void setDescription(String desc)      { this.description = desc; }
    public void setAuthor(User author)           { this.author = author; }
    public void setCreatedAt(Long createdAt)     { this.createdAt = createdAt; }
}
