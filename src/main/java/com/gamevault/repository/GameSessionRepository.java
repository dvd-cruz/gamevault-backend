package com.gamevault.repository;

import com.gamevault.model.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    List<GameSession> findByPlaythroughIdOrderByDateDesc(Long playthroughId);
}
