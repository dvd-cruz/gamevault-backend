package com.gamevault.controller;

import com.gamevault.dto.CalendarEventResponse;
import com.gamevault.model.CalendarEvent;
import com.gamevault.repository.CalendarEventRepository;
import com.gamevault.repository.UserRepository;
import com.gamevault.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar-events")
public class CalendarEventController {

    private final CalendarEventRepository repo;
    private final UserRepository userRepo;

    public CalendarEventController(CalendarEventRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    private void requireAdmin(UserPrincipal principal, String message) {
        var user = userRepo.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
        if (!user.isAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    /** All calendar events — visible to everyone on the Discover calendar. */
    @GetMapping
    public List<CalendarEventResponse> list() {
        return repo.findAllByOrderByMonthAscDayAsc().stream().map(CalendarEventResponse::from).toList();
    }

    /** Admin: add a celebration/date. Recurring when `year` is omitted. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalendarEventResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                        @RequestBody Map<String, Object> body) {
        requireAdmin(principal, "Apenas administradores podem adicionar datas");

        String label = body.get("label") != null ? body.get("label").toString().trim() : "";
        if (label.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome é obrigatório");
        Integer month = num(body.get("month"));
        Integer day   = num(body.get("day"));
        if (month == null || month < 1 || month > 12) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mês inválido");
        if (day == null || day < 1 || day > 31)       throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dia inválido");

        CalendarEvent e = new CalendarEvent();
        e.setLabel(label);
        e.setEmoji(body.get("emoji") != null && !body.get("emoji").toString().isBlank() ? body.get("emoji").toString().trim() : "🎉");
        e.setMonth(month);
        e.setDay(day);
        e.setYear(num(body.get("year"))); // null → recurring
        e.setCreatedAt(System.currentTimeMillis());
        return CalendarEventResponse.from(repo.save(e));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        requireAdmin(principal, "Apenas administradores podem remover datas");
        repo.deleteById(id);
    }

    private static Integer num(Object o) {
        return o instanceof Number n ? n.intValue() : null;
    }
}
