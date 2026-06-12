package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "unlocked_trophies")
public class UnlockedTrophy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trophy_id", nullable = false)
    private Trophy trophy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private Long unlockedAt;

    /** When the unlock was recorded in this system (equals unlockedAt for manual unlocks, differs for Steam syncs). */
    private Long markedAt;

    /** How this unlock was recorded: "manual" | "steam" | "psn". */
    private String source;

    public UnlockedTrophy() {}

    public Long getId()           { return id; }
    public Trophy getTrophy()     { return trophy; }
    public Game getGame()         { return game; }
    public Long getUnlockedAt()   { return unlockedAt; }
    public Long getMarkedAt()     { return markedAt != null ? markedAt : unlockedAt; }
    public String getSource()     { return source != null ? source : "manual"; }

    public void setTrophy(Trophy trophy)         { this.trophy = trophy; }
    public void setGame(Game game)               { this.game = game; }
    public void setUnlockedAt(Long unlockedAt)   { this.unlockedAt = unlockedAt; }
    public void setMarkedAt(Long markedAt)       { this.markedAt = markedAt; }
    public void setSource(String source)         { this.source = source; }
}
