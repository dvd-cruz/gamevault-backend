package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "game_list_items")
public class GameListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String emoji;
    private String genre;

    @Column(columnDefinition = "TEXT")
    private String note;

    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private GameList gameList;

    public GameListItem() {}

    public Long getId()           { return id; }
    public String getTitle()      { return title; }
    public String getEmoji()      { return emoji; }
    public String getGenre()      { return genre; }
    public String getNote()       { return note; }
    public Integer getPosition()  { return position; }
    public GameList getGameList() { return gameList; }

    public void setTitle(String title)         { this.title = title; }
    public void setEmoji(String emoji)         { this.emoji = emoji; }
    public void setGenre(String genre)         { this.genre = genre; }
    public void setNote(String note)           { this.note = note; }
    public void setPosition(Integer position)  { this.position = position; }
    public void setGameList(GameList gameList) { this.gameList = gameList; }
}
