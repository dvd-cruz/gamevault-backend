package com.gamevault.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "playthroughs")
public class Playthrough {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    private boolean speedrun;
    private boolean modded;
    private boolean completed;

    /** Total hours spent on this playthrough (optional). */
    private Double hours;

    private String platform;
    private String difficulty;
    private String language;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @OneToMany(mappedBy = "playthrough", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameSession> sessions = new ArrayList<>();

    public Playthrough() {}

    public List<GameSession> getSessions() { return sessions; }

    public Long getId()           { return id; }
    public String getType()       { return type; }
    public boolean isSpeedrun()   { return speedrun; }
    public boolean isModded()     { return modded; }
    public boolean isCompleted()  { return completed; }
    public Double getHours()      { return hours; }
    public String getPlatform()   { return platform; }
    public String getDifficulty() { return difficulty; }
    public String getLanguage()   { return language; }
    public String getNotes()      { return notes; }
    public Game getGame()         { return game; }

    public void setType(String type)             { this.type = type; }
    public void setSpeedrun(boolean speedrun)    { this.speedrun = speedrun; }
    public void setModded(boolean modded)        { this.modded = modded; }
    public void setCompleted(boolean completed)  { this.completed = completed; }
    public void setHours(Double hours)           { this.hours = hours; }
    public void setPlatform(String platform)     { this.platform = platform; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setLanguage(String language)     { this.language = language; }
    public void setNotes(String notes)           { this.notes = notes; }
    public void setGame(Game game)               { this.game = game; }
}
