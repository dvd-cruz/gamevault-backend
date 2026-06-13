package com.gamevault.model;

import jakarta.persistence.*;

/** RSVP — a user who joined a scheduled play session. */
@Entity
@Table(name = "event_participants", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
public class EventParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private GameEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long joinedAt;

    public EventParticipant() {}

    public Long getId()        { return id; }
    public GameEvent getEvent() { return event; }
    public User getUser()      { return user; }
    public Long getJoinedAt()  { return joinedAt; }

    public void setEvent(GameEvent event) { this.event = event; }
    public void setUser(User user)        { this.user = user; }
    public void setJoinedAt(Long joinedAt) { this.joinedAt = joinedAt; }
}
