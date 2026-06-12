package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String date;

    @Column(nullable = false)
    private Double hours;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playthrough_id", nullable = false)
    private Playthrough playthrough;

    public GameSession() {}

    public Long getId()                 { return id; }
    public String getDate()             { return date; }
    public Double getHours()            { return hours; }
    public String getNotes()            { return notes; }
    public Playthrough getPlaythrough() { return playthrough; }

    public void setDate(String date)               { this.date = date; }
    public void setHours(Double hours)              { this.hours = hours; }
    public void setNotes(String notes)              { this.notes = notes; }
    public void setPlaythrough(Playthrough playthrough) { this.playthrough = playthrough; }
}
