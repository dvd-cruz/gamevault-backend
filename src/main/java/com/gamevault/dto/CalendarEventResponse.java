package com.gamevault.dto;

import com.gamevault.model.CalendarEvent;

public record CalendarEventResponse(
        Long id,
        String label,
        String emoji,
        Integer month,
        Integer day,
        Integer year,
        boolean recurring
) {
    public static CalendarEventResponse from(CalendarEvent e) {
        return new CalendarEventResponse(e.getId(), e.getLabel(), e.getEmoji(),
                e.getMonth(), e.getDay(), e.getYear(), e.getYear() == null);
    }
}
