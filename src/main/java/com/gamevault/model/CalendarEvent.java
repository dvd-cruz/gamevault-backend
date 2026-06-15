package com.gamevault.model;

import jakarta.persistence.*;

/**
 * A celebration/date shown on the Discover calendar. If {@code year} is null the event is
 * recurring (every year on month/day); otherwise it happens once on that specific year.
 */
@Entity
@Table(name = "calendar_events")
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    private String emoji;

    @Column(name = "event_month", nullable = false)
    private Integer month; // 1-12

    @Column(name = "event_day", nullable = false)
    private Integer day;   // 1-31

    /** null = recurring every year; set = one-time on this specific year. */
    @Column(name = "event_year")
    private Integer year;

    @Column(nullable = false)
    private Long createdAt;

    public CalendarEvent() {}

    public Long getId()        { return id; }
    public String getLabel()   { return label; }
    public String getEmoji()   { return emoji; }
    public Integer getMonth()  { return month; }
    public Integer getDay()    { return day; }
    public Integer getYear()   { return year; }
    public Long getCreatedAt() { return createdAt; }

    public void setLabel(String label)       { this.label = label; }
    public void setEmoji(String emoji)       { this.emoji = emoji; }
    public void setMonth(Integer month)      { this.month = month; }
    public void setDay(Integer day)          { this.day = day; }
    public void setYear(Integer year)        { this.year = year; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
