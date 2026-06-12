package com.gamevault.repository;

import com.gamevault.model.Friendship;
import com.gamevault.model.Friendship.Status;
import com.gamevault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("select f from Friendship f where " +
           "(f.requester = :a and f.recipient = :b) or (f.requester = :b and f.recipient = :a)")
    Optional<Friendship> findBetween(User a, User b);

    List<Friendship> findByRecipientAndStatus(User recipient, Status status);

    @Query("select f from Friendship f where f.status = 'ACCEPTED' and (f.requester = :user or f.recipient = :user)")
    List<Friendship> findAcceptedFor(User user);
}
