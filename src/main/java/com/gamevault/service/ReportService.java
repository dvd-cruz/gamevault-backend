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

    public ReportService(ReportRepository reportRepo, UserRepository userRepo) {
        this.reportRepo = reportRepo;
        this.userRepo   = userRepo;
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
