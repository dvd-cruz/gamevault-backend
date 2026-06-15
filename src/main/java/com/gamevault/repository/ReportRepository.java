package com.gamevault.repository;

import com.gamevault.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    /** Unresolved first, then most recent. */
    List<Report> findAllByOrderByResolvedAscCreatedAtDesc();
}
