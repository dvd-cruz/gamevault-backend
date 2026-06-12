package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "friendships")
public class Friendship {

    public enum Status { PENDING, ACCEPTED, DECLINED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private Long createdAt;

    public Friendship() {}

    public Long getId()           { return id; }
    public User getRequester()    { return requester; }
    public User getRecipient()    { return recipient; }
    public Status getStatus()     { return status; }
    public Long getCreatedAt()    { return createdAt; }

    public void setRequester(User requester) { this.requester = requester; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    public void setStatus(Status status)     { this.status = status; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
