package com.gamevault.service;

import com.gamevault.dto.ReportRequest;
import com.gamevault.model.Report;
import com.gamevault.model.User;
import com.gamevault.repository.ReportRepository;
import com.gamevault.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportService {

    private final ReportRepository reportRepo;
    private final UserRepository userRepo;
    private final com.gamevault.repository.ActivityRepository activityRepo;

    public ReportService(ReportRepository reportRepo, UserRepository userRepo,
                         com.gamevault.repository.ActivityRepository activityRepo) {
        this.reportRepo = reportRepo;
        this.userRepo   = userRepo;
        this.activityRepo = activityRepo;
    }

    /** Reports a specific post/activity (the reported user is its author; a content snapshot is kept). */
    @Transactional
    public void reportPost(Long reporterId, Long activityId, ReportRequest req) {
        User reporter = findUser(reporterId);
        com.gamevault.model.Activity activity = activityRepo.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicação não encontrada"));
        User reported = activity.getActor();
        if (reporter.getId().equals(reported.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não podes denunciar a tua própria publicação");

        String snapshot = activity.getMessage() != null ? activity.getMessage() : "";
        if (activity.getImage() != null) snapshot += "\n[imagem] " + activity.getImage();
        if (activity.getCatalogGame() != null) snapshot = "(" + activity.getCatalogGame().getTitle() + ") " + snapshot;

        Report r = new Report();
        r.setReporter(reporter);
        r.setReported(reported);
        r.setReason(req.reason());
        r.setDescription(req.description());
        r.setActivityId(activityId);
        r.setContentSnapshot(snapshot.length() > 2000 ? snapshot.substring(0, 2000) : snapshot);
        r.setCreatedAt(System.currentTimeMillis());
        reportRepo.save(r);
    }

    /** Admin: all reports, unresolved first. */
    public java.util.List<com.gamevault.dto.ReportResponse> listReports(Long adminId) {
        requireAdmin(adminId);
        return reportRepo.findAllByOrderByResolvedAscCreatedAtDesc()
                .stream().map(com.gamevault.dto.ReportResponse::from).toList();
    }

    @Transactional
    public void resolveReport(Long adminId, Long reportId, boolean resolved) {
        requireAdmin(adminId);
        Report r = reportRepo.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Denúncia não encontrada"));
        r.setResolved(resolved);
        r.setResolvedAt(resolved ? System.currentTimeMillis() : null);
        reportRepo.save(r);
    }

    /** Admin: suspend or reinstate a user (suspended users can't log in). */
    @Transactional
    public void setSuspended(Long adminId, String username, boolean suspended) {
        requireAdmin(adminId);
        User target = findByUsername(username);
        if (target.isAdmin())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não podes suspender um administrador");
        target.setSuspended(suspended);
        userRepo.save(target);
    }

    private void requireAdmin(Long userId) {
        if (!findUser(userId).isAdmin())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores podem moderar");
    }

    @Transactional
    public void report(Long reporterId, String reportedUsername, ReportRequest req) {
        User reporter = findUser(reporterId);
        User reported = findByUsername(reportedUsername);

        if (reporter.getId().equals(reported.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não podes denunciar-te a ti próprio");

        Report r = new Report();
        r.setReporter(reporter);
        r.setReported(reported);
        r.setReason(req.reason());
        r.setDescription(req.description());
        r.setCreatedAt(System.currentTimeMillis());
        reportRepo.save(r);
    }

    private User findUser(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
    }

    private User findByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
    }
}
