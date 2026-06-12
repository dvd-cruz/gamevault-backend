package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "blocks")
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(nullable = false)
    private Long createdAt;

    public Block() {}

    public Long getId()        { return id; }
    public User getBlocker()   { return blocker; }
    public User getBlocked()   { return blocked; }
    public Long getCreatedAt() { return createdAt; }

    public void setBlocker(User blocker)     { this.blocker = blocker; }
    public void setBlocked(User blocked)     { this.blocked = blocked; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
