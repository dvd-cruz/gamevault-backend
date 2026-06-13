package com.gamevault.repository;

import com.gamevault.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    /** Groups the given user belongs to (as member — owners are members too). */
    @Query("SELECT m.group FROM GroupMember m WHERE m.user.id = :userId ORDER BY m.group.createdAt DESC")
    List<UserGroup> findByMember(@Param("userId") Long userId);
}
