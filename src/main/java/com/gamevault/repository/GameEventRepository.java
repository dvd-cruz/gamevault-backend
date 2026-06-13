package com.gamevault.repository;

import com.gamevault.model.GameEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameEventRepository extends JpaRepository<GameEvent, Long> {

    /** Events hosted by any of the given users: scheduled-and-upcoming OR open (no time set). Soonest first, timeless last. */
    @Query("SELECT e FROM GameEvent e WHERE e.host.id IN :hostIds AND (e.scheduledAt IS NULL OR e.scheduledAt >= :since) " +
           "ORDER BY CASE WHEN e.scheduledAt IS NULL THEN 1 ELSE 0 END, e.scheduledAt ASC, e.createdAt DESC")
    List<GameEvent> findUpcomingByHosts(@Param("hostIds") List<Long> hostIds, @Param("since") long since);

    /** Public LFG events for one game: scheduled-and-upcoming OR open. Shown on the game page to everyone. */
    @Query("SELECT e FROM GameEvent e WHERE e.catalogGame.id = :gameId AND e.visibility = 'public' " +
           "AND (e.scheduledAt IS NULL OR e.scheduledAt >= :since) " +
           "ORDER BY CASE WHEN e.scheduledAt IS NULL THEN 1 ELSE 0 END, e.scheduledAt ASC, e.createdAt DESC")
    List<GameEvent> findPublicByGame(@Param("gameId") Long gameId, @Param("since") long since);
}
