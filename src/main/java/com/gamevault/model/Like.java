package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "likes", uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "actor_id"}))
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(nullable = false)
    private Long createdAt;

    /** Whether the like is currently active (false = the user has unliked it). Kept so a re-like doesn't re-trigger a notification. */
    @Column(nullable = false)
    private boolean active = true;

    public Like() {}

    public Long getId()           { return id; }
    public User getActor()        { return actor; }
    public Activity getActivity() { return activity; }
    public Long getCreatedAt()    { return createdAt; }
    public boolean isActive()     { return active; }

    public void setActor(User actor)         { this.actor = actor; }
    public void setActivity(Activity activity) { this.activity = activity; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public void setActive(boolean active)    { this.active = active; }
}
