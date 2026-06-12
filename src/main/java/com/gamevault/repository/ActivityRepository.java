package com.gamevault.repository;

import com.gamevault.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @Query("select a from Activity a where a.actor.id in :actorIds order by a.createdAt desc")
    List<Activity> findByActorIdInOrderByCreatedAtDesc(List<Long> actorIds);

    List<Activity> findByActorIdOrderByCreatedAtDesc(Long actorId);

    Optional<Activity> findFirstByActorIdAndCatalogGameIdAndTypeOrderByCreatedAtDesc(Long actorId, Long catalogGameId, String type);

    Optional<Activity> findByActorIdAndCatalogGameIdAndType(Long actorId, Long catalogGameId, String type);

    List<Activity> findByCatalogGameIdAndTypeOrderByCreatedAtDesc(Long catalogGameId, String type);

    List<Activity> findByCatalogGameIdAndTypeInOrderByCreatedAtDesc(Long catalogGameId, List<String> types);

    List<Activity> findByActorIdAndTypeOrderByCreatedAtDesc(Long actorId, String type);

    List<Activity> findByActorIdAndTypeInOrderByCreatedAtDesc(Long actorId, List<String> types);

    boolean existsByActorIdAndCatalogGameIdAndTypeAndTrophyId(Long actorId, Long catalogGameId, String type, Long trophyId);
}
