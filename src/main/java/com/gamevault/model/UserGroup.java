package com.gamevault.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** A small private club of users with its own post feed (family, work friends, …). */
@Entity
@Table(name = "user_groups")
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private Long createdAt;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<GroupPost> posts = new ArrayList<>();

    public UserGroup() {}

    public Long getId()              { return id; }
    public String getName()          { return name; }
    public String getDescription()   { return description; }
    public User getOwner()           { return owner; }
    public Long getCreatedAt()       { return createdAt; }
    public List<GroupMember> getMembers() { return members; }
    public List<GroupPost> getPosts()     { return posts; }

    public void setName(String name)               { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setOwner(User owner)               { this.owner = owner; }
    public void setCreatedAt(Long createdAt)       { this.createdAt = createdAt; }
}
