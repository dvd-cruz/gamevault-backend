package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(columnDefinition = "TEXT")
    private String avatar;
    @Column(columnDefinition = "TEXT")
    private String bio;
    private String location;
    private String steamId;

    @Column(columnDefinition = "TEXT")
    private String npsso;
    private String psnAccountId;

    @Column(nullable = false)
    private Long joinedAt;

    @Column(nullable = false)
    private boolean admin = false;

    @Column(nullable = false)
    private boolean privateProfile = false;

    /** Suspended by a moderator — cannot log in. */
    @Column(nullable = false)
    private boolean suspended = false;

    @ManyToMany
    @JoinTable(
            name = "user_favorite_franchises",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "franchise_id")
    )
    private java.util.List<Franchise> favoriteFranchises = new java.util.ArrayList<>();

    public User() {}

    public Long getId()             { return id; }
    public String getName()         { return name; }
    public String getUsername()     { return username; }
    public String getEmail()        { return email; }
    public String getPassword()     { return password; }
    public String getAvatar()       { return avatar; }
    public String getBio()          { return bio; }
    public String getLocation()     { return location; }
    public String getSteamId()      { return steamId; }
    public String getNpsso()        { return npsso; }
    public String getPsnAccountId() { return psnAccountId; }
    public Long getJoinedAt()       { return joinedAt; }
    public boolean isAdmin()        { return admin; }

    public void setName(String name)         { this.name = name; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email)       { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setAvatar(String avatar)     { this.avatar = avatar; }
    public void setBio(String bio)           { this.bio = bio; }
    public void setLocation(String location) { this.location = location; }
    public void setSteamId(String steamId)       { this.steamId = steamId; }
    public void setNpsso(String npsso)           { this.npsso = npsso; }
    public void setPsnAccountId(String id)       { this.psnAccountId = id; }
    public void setJoinedAt(Long joinedAt)   { this.joinedAt = joinedAt; }
    public void setAdmin(boolean admin)      { this.admin = admin; }
    public boolean isPrivateProfile()               { return privateProfile; }
    public void setPrivateProfile(boolean privateProfile) { this.privateProfile = privateProfile; }
    public boolean isSuspended()                    { return suspended; }
    public void setSuspended(boolean suspended)     { this.suspended = suspended; }

    public java.util.List<Franchise> getFavoriteFranchises() { return favoriteFranchises; }
    public void setFavoriteFranchises(java.util.List<Franchise> favoriteFranchises) { this.favoriteFranchises = favoriteFranchises; }
}
