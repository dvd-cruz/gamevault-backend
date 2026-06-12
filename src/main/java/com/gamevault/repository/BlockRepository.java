package com.gamevault.repository;

import com.gamevault.model.Block;
import com.gamevault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {

    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);

    List<Block> findByBlocker(User blocker);

    @Query("select case when count(b) > 0 then true else false end from Block b " +
           "where (b.blocker = :a and b.blocked = :b) or (b.blocker = :b and b.blocked = :a)")
    boolean existsBetween(User a, User b);
}
