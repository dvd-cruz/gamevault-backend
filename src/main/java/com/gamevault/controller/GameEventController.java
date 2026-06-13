package com.gamevault.controller;

import com.gamevault.dto.GameEventResponse;
import com.gamevault.model.EventParticipant;
import com.gamevault.model.Friendship;
import com.gamevault.model.GameEvent;
import com.gamevault.model.User;
import com.gamevault.repository.FriendshipRepository;
import com.gamevault.repository.GameCatalogRepository;
import com.gamevault.repository.GameEventRepository;
import com.gamevault.repository.UserRepository;
import com.gamevault.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class GameEventController {

    private final GameEventRepository eventRepo;
    private final GameCatalogRepository catalogRepo;
    private final FriendshipRepository friendshipRepo;
    private final UserRepository userRepo;

    public GameEventController(GameEventRepository eventRepo, GameCatalogRepository catalogRepo,
                               FriendshipRepository friendshipRepo, UserRepository userRepo) {
        this.eventRepo = eventRepo;
        this.catalogRepo = catalogRepo;
        this.friendshipRepo = friendshipRepo;
        this.userRepo = userRepo;
    }

    /** Upcoming events visible to me: mine plus my friends'. Soonest first. */
    @GetMapping
    public List<GameEventResponse> upcoming(@AuthenticationPrincipal UserPrincipal principal) {
        User me = userRepo.findById(principal.getId()).orElseThrow();
        List<Long> hostIds = new ArrayList<>();
        hostIds.add(me.getId());
        for (Friendship f : friendshipRepo.findAcceptedFor(me)) {
            User other = f.getRequester().getId().equals(me.getId()) ? f.getRecipient() : f.getRequester();
            hostIds.add(other.getId());
        }
        return eventRepo.findUpcomingByHosts(hostIds, System.currentTimeMillis())
                .stream().map(e -> GameEventResponse.from(e, me.getId())).toList();
    }

    /** Public "looking for group" events for a specific game — shown on the game page to everyone. */
    @GetMapping("/game/{catalogGameId}")
    public List<GameEventResponse> forGame(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long catalogGameId) {
        return eventRepo.findPublicByGame(catalogGameId, System.currentTimeMillis())
                .stream().map(e -> GameEventResponse.from(e, principal.getId())).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public GameEventResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestBody Map<String, Object> body) {
        Long catalogGameId = body.get("catalogGameId") != null ? ((Number) body.get("catalogGameId")).longValue() : null;
        Long scheduledAt   = body.get("scheduledAt")   != null ? ((Number) body.get("scheduledAt")).longValue()   : null;
        String note        = body.get("note")          != null ? body.get("note").toString().trim()               : null;
        String visibility  = "public".equals(body.get("visibility")) ? "public" : "friends";
        String platform    = body.get("platform") != null ? body.get("platform").toString().trim() : null;
        String mode        = "serious".equals(body.get("mode")) ? "serious" : "casual".equals(body.get("mode")) ? "casual" : null;
        if (catalogGameId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolhe um jogo");
        // time is optional ("procuro grupo, sem hora definida"); when given it must be in the future
        if (scheduledAt != null && scheduledAt <= System.currentTimeMillis())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data tem de ser no futuro");

        GameEvent e = new GameEvent();
        e.setHost(userRepo.findById(principal.getId()).orElseThrow());
        e.setCatalogGame(catalogRepo.findById(catalogGameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jogo não encontrado")));
        e.setNote(note == null || note.isBlank() ? null : note);
        e.setScheduledAt(scheduledAt);
        e.setVisibility(visibility);
        e.setPlatform(platform == null || platform.isBlank() ? null : platform);
        e.setMode(mode);
        e.setCreatedAt(System.currentTimeMillis());
        e = eventRepo.save(e);

        // the host automatically attends their own session
        EventParticipant host = new EventParticipant();
        host.setEvent(e);
        host.setUser(e.getHost());
        host.setJoinedAt(System.currentTimeMillis());
        e.getParticipants().add(host);
        return GameEventResponse.from(eventRepo.save(e), principal.getId());
    }

    @PostMapping("/{id}/join")
    @Transactional
    public GameEventResponse join(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        GameEvent e = find(id);
        User me = userRepo.findById(principal.getId()).orElseThrow();
        boolean already = e.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(me.getId()));
        if (!already) {
            EventParticipant p = new EventParticipant();
            p.setEvent(e);
            p.setUser(me);
            p.setJoinedAt(System.currentTimeMillis());
            e.getParticipants().add(p);
            e = eventRepo.save(e);
        }
        return GameEventResponse.from(e, me.getId());
    }

    @PostMapping("/{id}/leave")
    @Transactional
    public GameEventResponse leave(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        GameEvent e = find(id);
        if (e.getHost().getId().equals(principal.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O anfitrião não pode sair — apaga o evento");
        e.getParticipants().removeIf(p -> p.getUser().getId().equals(principal.getId()));
        return GameEventResponse.from(eventRepo.save(e), principal.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        GameEvent e = find(id);
        if (!e.getHost().getId().equals(principal.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o anfitrião pode apagar o evento");
        eventRepo.delete(e);
    }

    private GameEvent find(Long id) {
        return eventRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));
    }
}
