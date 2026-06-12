package com.gamevault.repository;

import com.gamevault.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByActivityIdOrderByCreatedAtAsc(Long activityId);

    long countByActivityId(Long activityId);

    void deleteByActivityId(Long activityId);

    @Query("select c from Comment c where c.activity.actor.id = :ownerId and c.actor.id <> :ownerId order by c.createdAt desc")
    List<Comment> findReceivedByOwner(Long ownerId);
}
