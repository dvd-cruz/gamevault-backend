package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "group_members", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private UserGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long joinedAt;

    public GroupMember() {}

    public Long getId()         { return id; }
    public UserGroup getGroup() { return group; }
    public User getUser()       { return user; }
    public Long getJoinedAt()   { return joinedAt; }

    public void setGroup(UserGroup group) { this.group = group; }
    public void setUser(User user)        { this.user = user; }
    public void setJoinedAt(Long joinedAt) { this.joinedAt = joinedAt; }
}
