package com.gamevault.controller;

import com.gamevault.dto.ReportRequest;
import com.gamevault.security.UserPrincipal;
import com.gamevault.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@AuthenticationPrincipal UserPrincipal principal,
                        @PathVariable String username,
                        @Valid @RequestBody ReportRequest req) {
        reportService.report(principal.getId(), username, req);
    }

    /** Report a specific post/activity (covers its text and images). */
    @PostMapping("/post/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportPost(@AuthenticationPrincipal UserPrincipal principal,
                           @PathVariable Long activityId,
                           @Valid @RequestBody ReportRequest req) {
        reportService.reportPost(principal.getId(), activityId, req);
    }

    /** Admin: moderation queue (unresolved first). */
    @GetMapping
    public java.util.List<com.gamevault.dto.ReportResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return reportService.listReports(principal.getId());
    }

    /** Admin: mark a report resolved/unresolved. */
    @PostMapping("/{id}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolve(@AuthenticationPrincipal UserPrincipal principal,
                        @PathVariable Long id,
                        @RequestBody(required = false) java.util.Map<String, Object> body) {
        boolean resolved = body == null || body.get("resolved") == null || Boolean.TRUE.equals(body.get("resolved"));
        reportService.resolveReport(principal.getId(), id, resolved);
    }

    /** Admin: suspend or reinstate a user. */
    @PostMapping("/users/{username}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(@AuthenticationPrincipal UserPrincipal principal,
                        @PathVariable String username,
                        @RequestBody(required = false) java.util.Map<String, Object> body) {
        boolean suspended = body == null || body.get("suspended") == null || Boolean.TRUE.equals(body.get("suspended"));
        reportService.setSuspended(principal.getId(), username, suspended);
    }
}
