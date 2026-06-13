package com.gamevault.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** A scheduled play session ("quem quer jogar X sexta às 21h?") hosted by a user, visible to their friends. */
@Entity
@Table(name = "game_events")
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_game_id", nullable = false)
    private GameCatalog catalogGame;

    @Column(columnDefinition = "TEXT")
    private String note;

    /** When the session happens — null means "procuro grupo, sem hora definida". */
    private Long scheduledAt;

    /** "friends" (only friends see it) or "public" (anyone, shown on the game page). */
    @Column(nullable = false)
    private String visibility = "friends";

    /** Platform for the session (e.g. "PS5") — one of the game's platforms, optional. */
    private String platform;

    /** "casual" or "serious" — the intended gameplay tone, optional. */
    private String mode;

    @Column(nullable = false)
    private Long createdAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventParticipant> participants = new ArrayList<>();

    public GameEvent() {}

    public Long getId()                  { return id; }
    public User getHost()                { return host; }
    public GameCatalog getCatalogGame()  { return catalogGame; }
    public String getNote()              { return note; }
    public Long getScheduledAt()         { return scheduledAt; }
    public String getVisibility()        { return visibility; }
    public String getPlatform()          { return platform; }
    public String getMode()              { return mode; }
    public Long getCreatedAt()           { return createdAt; }
    public List<EventParticipant> getParticipants() { return participants; }

    public void setHost(User host)                     { this.host = host; }
    public void setCatalogGame(GameCatalog catalogGame) { this.catalogGame = catalogGame; }
    public void setNote(String note)                   { this.note = note; }
    public void setScheduledAt(Long scheduledAt)       { this.scheduledAt = scheduledAt; }
    public void setVisibility(String visibility)       { this.visibility = visibility; }
    public void setPlatform(String platform)           { this.platform = platform; }
    public void setMode(String mode)                   { this.mode = mode; }
    public void setCreatedAt(Long createdAt)           { this.createdAt = createdAt; }
}
