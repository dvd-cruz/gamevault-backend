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
}
