package com.gamevault.model;

import jakarta.persistence.*;

@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @Column(nullable = false)
    private String gameTitle;

    private String gameEmoji;
    private String genre;
    private String platform;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private Long sentAt;

    private boolean read = false;

    public Recommendation() {}

    public Long getId()           { return id; }
    public User getFromUser()     { return fromUser; }
    public User getToUser()       { return toUser; }
    public String getGameTitle()  { return gameTitle; }
    public String getGameEmoji()  { return gameEmoji; }
    public String getGenre()      { return genre; }
    public String getPlatform()   { return platform; }
    public String getMessage()    { return message; }
    public Long getSentAt()       { return sentAt; }
    public boolean isRead()       { return read; }

    public void setFromUser(User fromUser)     { this.fromUser = fromUser; }
    public void setToUser(User toUser)         { this.toUser = toUser; }
    public void setGameTitle(String gameTitle) { this.gameTitle = gameTitle; }
    public void setGameEmoji(String gameEmoji) { this.gameEmoji = gameEmoji; }
    public void setGenre(String genre)         { this.genre = genre; }
    public void setPlatform(String platform)   { this.platform = platform; }
    public void setMessage(String message)     { this.message = message; }
    public void setSentAt(Long sentAt)         { this.sentAt = sentAt; }
    public void setRead(boolean read)          { this.read = read; }
}
