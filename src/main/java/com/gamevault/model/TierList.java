package com.gamevault.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tier_lists")
public class TierList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private Long createdAt;

    private Long updatedAt;

    @OneToMany(mappedBy = "tierList", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<TierListEntry> entries = new ArrayList<>();

    public TierList() {}

    public Long getId()                  { return id; }
    public String getTitle()             { return title; }
    public User getOwner()               { return owner; }
    public Long getCreatedAt()           { return createdAt; }
    public Long getUpdatedAt()           { return updatedAt; }
    public List<TierListEntry> getEntries() { return entries; }

    public void setTitle(String title)       { this.title = title; }
    public void setOwner(User owner)         { this.owner = owner; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
