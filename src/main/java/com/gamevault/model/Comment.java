package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private Long createdAt;

    public Comment() {}

    public Long getId()           { return id; }
    public User getActor()        { return actor; }
    public Activity getActivity() { return activity; }
    public String getText()       { return text; }
    public Long getCreatedAt()    { return createdAt; }

    public void setActor(User actor)           { this.actor = actor; }
    public void setActivity(Activity activity) { this.activity = activity; }
    public void setText(String text)           { this.text = text; }
    public void setCreatedAt(Long createdAt)   { this.createdAt = createdAt; }
}
