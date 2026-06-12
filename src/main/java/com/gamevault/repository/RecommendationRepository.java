package com.gamevault.repository;

import com.gamevault.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByToUserIdOrderBySentAtDesc(Long toUserId);

    long countByToUserIdAndReadFalse(Long toUserId);
}
