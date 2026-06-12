package com.gamevault.repository;

import com.gamevault.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    long countByActivityIdAndActiveTrue(Long activityId);

    Optional<Like> findByActivityIdAndActorId(Long activityId, Long actorId);

    boolean existsByActivityIdAndActorIdAndActiveTrue(Long activityId, Long actorId);

    void deleteByActivityId(Long activityId);

    @Query("select l from Like l where l.activity.actor.id = :ownerId and l.actor.id <> :ownerId and l.active = true order by l.createdAt desc")
    List<Like> findReceivedByOwner(Long ownerId);
}
