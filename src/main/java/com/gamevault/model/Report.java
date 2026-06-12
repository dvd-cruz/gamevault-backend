package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_id", nullable = false)
    private User reported;

    @Column(nullable = false)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long createdAt;

    public Report() {}

    public Long getId()           { return id; }
    public User getReporter()     { return reporter; }
    public User getReported()     { return reported; }
    public String getReason()     { return reason; }
    public String getDescription(){ return description; }
    public Long getCreatedAt()    { return createdAt; }

    public void setReporter(User reporter)       { this.reporter = reporter; }
    public void setReported(User reported)       { this.reported = reported; }
    public void setReason(String reason)         { this.reason = reason; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedAt(Long createdAt)     { this.createdAt = createdAt; }
}
