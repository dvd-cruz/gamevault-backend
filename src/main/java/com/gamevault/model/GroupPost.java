package com.gamevault.model;

import jakarta.persistence.*;

/** A post inside a private group's feed. */
@Entity
@Table(name = "group_posts")
public class GroupPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private UserGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** Rich-text HTML from the editor. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(nullable = false)
    private Long createdAt;

    public GroupPost() {}

    public Long getId()         { return id; }
    public UserGroup getGroup() { return group; }
    public User getAuthor()     { return author; }
    public String getMessage()  { return message; }
    public Long getCreatedAt()  { return createdAt; }

    public void setGroup(UserGroup group)   { this.group = group; }
    public void setAuthor(User author)      { this.author = author; }
    public void setMessage(String message)  { this.message = message; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
