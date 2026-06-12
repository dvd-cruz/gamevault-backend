package com.gamevault.service;

import com.gamevault.dto.UserResponse;
import com.gamevault.model.Block;
import com.gamevault.model.User;
import com.gamevault.repository.BlockRepository;
import com.gamevault.repository.FriendshipRepository;
import com.gamevault.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BlockService {

    private final BlockRepository blockRepo;
    private final FriendshipRepository friendRepo;
    private final UserRepository userRepo;

    public BlockService(BlockRepository blockRepo, FriendshipRepository friendRepo, UserRepository userRepo) {
        this.blockRepo  = blockRepo;
        this.friendRepo = friendRepo;
        this.userRepo   = userRepo;
    }

    public List<UserResponse> listBlocked(Long userId) {
        return blockRepo.findByBlocker(findUser(userId)).stream()
                .map(b -> UserResponse.from(b.getBlocked()))
                .toList();
    }

    @Transactional
    public void block(Long userId, String targetUsername) {
        User blocker = findUser(userId);
        User blocked = findByUsername(targetUsername);

        if (blocker.getId().equals(blocked.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não podes bloquear-te a ti próprio");

        if (blockRepo.findByBlockerAndBlocked(blocker, blocked).isPresent())
            return;

        Block b = new Block();
        b.setBlocker(blocker);
        b.setBlocked(blocked);
        b.setCreatedAt(System.currentTimeMillis());
        blockRepo.save(b);

        friendRepo.findBetween(blocker, blocked).ifPresent(friendRepo::delete);
    }

    @Transactional
    public void unblock(Long userId, String targetUsername) {
        User blocker = findUser(userId);
        User blocked = findByUsername(targetUsername);

        Block b = blockRepo.findByBlockerAndBlocked(blocker, blocked)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não está bloqueado"));
        blockRepo.delete(b);
    }

    private User findUser(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
    }

    private User findByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
    }
}
