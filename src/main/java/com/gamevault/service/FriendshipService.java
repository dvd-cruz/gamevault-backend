package com.gamevault.service;

import com.gamevault.dto.FriendRequestResponse;
import com.gamevault.dto.FriendStatusResponse;
import com.gamevault.dto.NotificationResponse;
import com.gamevault.dto.UserResponse;
import com.gamevault.model.Friendship;
import com.gamevault.model.Friendship.Status;
import com.gamevault.model.User;
import com.gamevault.repository.BlockRepository;
import com.gamevault.repository.FriendshipRepository;
import com.gamevault.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class FriendshipService {

    private final FriendshipRepository friendRepo;
    private final BlockRepository blockRepo;
    private final UserRepository userRepo;
    private final SseEmitterRegistry sseRegistry;

    public FriendshipService(FriendshipRepository friendRepo, BlockRepository blockRepo, UserRepository userRepo, SseEmitterRegistry sseRegistry) {
        this.friendRepo = friendRepo;
        this.blockRepo  = blockRepo;
        this.userRepo   = userRepo;
        this.sseRegistry = sseRegistry;
    }

    public List<UserResponse> listFriends(Long userId) {
        User user = findUser(userId);
        return friendRepo.findAcceptedFor(user).stream()
                .map(f -> f.getRequester().getId().equals(userId) ? f.getRecipient() : f.getRequester())
                .map(UserResponse::from)
                .toList();
    }

    public List<FriendRequestResponse> pendingRequests(Long userId) {
        User user = findUser(userId);
        return friendRepo.findByRecipientAndStatus(user, Status.PENDING).stream()
                .map(FriendRequestResponse::from)
                .toList();
    }

    public FriendStatusResponse statusWith(Long userId, String otherUsername) {
        User user  = findUser(userId);
        User other = findByUsername(otherUsername);

        if (user.getId().equals(other.getId())) return new FriendStatusResponse("SELF", null);

        Optional<Friendship> existing = friendRepo.findBetween(user, other);
        if (existing.isEmpty()) return new FriendStatusResponse("NONE", null);

        Friendship f = existing.get();
        return switch (f.getStatus()) {
            case ACCEPTED -> new FriendStatusResponse("FRIENDS", f.getId());
            case DECLINED -> new FriendStatusResponse("NONE", null);
            case PENDING  -> f.getRequester().getId().equals(userId)
                    ? new FriendStatusResponse("PENDING_SENT", f.getId())
                    : new FriendStatusResponse("PENDING_RECEIVED", f.getId());
        };
    }

    @Transactional
    public FriendRequestResponse sendRequest(Long userId, String targetUsername) {
        User requester = findUser(userId);
        User recipient = findByUsername(targetUsername);

        if (requester.getId().equals(recipient.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não podes adicionar-te a ti próprio");

        if (blockRepo.existsBetween(requester, recipient))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não é possível enviar pedido a este utilizador");

        Optional<Friendship> existing = friendRepo.findBetween(requester, recipient);
        if (existing.isPresent() && existing.get().getStatus() != Status.DECLINED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um pedido ou amizade com este utilizador");

        Friendship f = existing.orElseGet(Friendship::new);
        f.setRequester(requester);
        f.setRecipient(recipient);
        f.setStatus(Status.PENDING);
        f.setCreatedAt(System.currentTimeMillis());

        FriendRequestResponse result = FriendRequestResponse.from(friendRepo.save(f));
        sseRegistry.push(recipient.getId(), new NotificationResponse(
                "freq-" + result.id(), "friend_request", System.currentTimeMillis(),
                requester.getUsername(), requester.getName(), requester.getAvatar(),
                "enviou-te um pedido de amizade",
                null, null, null, result.id()
        ));
        return result;
    }

    @Transactional
    public void respondToRequest(Long userId, Long requestId, boolean accept) {
        Friendship f = friendRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        if (!f.getRecipient().getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        if (f.getStatus() != Status.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido já foi respondido");

        f.setStatus(accept ? Status.ACCEPTED : Status.DECLINED);
        friendRepo.save(f);
    }

    @Transactional
    public void removeFriendOrCancelRequest(Long userId, String otherUsername) {
        User user  = findUser(userId);
        User other = findByUsername(otherUsername);

        Friendship f = friendRepo.findBetween(user, other)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não existe relação com este utilizador"));

        friendRepo.delete(f);
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
