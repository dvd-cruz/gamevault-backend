package com.gamevault.dto;

import com.gamevault.model.Report;

public record ReportResponse(
        Long id,
        String reason,
        String description,
        Long createdAt,
        boolean resolved,
        Long resolvedAt,
        String reporterUsername,
        String reporterName,
        String reportedUsername,
        String reportedName,
        String reportedAvatar,
        boolean reportedSuspended,
        Long activityId,
        String contentSnapshot
) {
    public static ReportResponse from(Report r) {
        return new ReportResponse(
                r.getId(), r.getReason(), r.getDescription(), r.getCreatedAt(),
                r.isResolved(), r.getResolvedAt(),
                r.getReporter().getUsername(), r.getReporter().getName(),
                r.getReported().getUsername(), r.getReported().getName(), r.getReported().getAvatar(),
                r.getReported().isSuspended(),
                r.getActivityId(), r.getContentSnapshot()
        );
    }
}
