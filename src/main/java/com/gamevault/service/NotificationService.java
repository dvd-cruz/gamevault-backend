package com.gamevault.service;

import com.gamevault.dto.NotificationResponse;
import com.gamevault.model.Comment;
import com.gamevault.model.Friendship;
import com.gamevault.model.Like;
import com.gamevault.model.User;
import com.gamevault.repository.CommentRepository;
import com.gamevault.repository.FriendshipRepository;
import com.gamevault.repository.LikeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class NotificationService {

    private final FriendshipRepository friendshipRepo;
    private final LikeRepository likeRepo;
    private final CommentRepository commentRepo;

    public NotificationService(FriendshipRepository friendshipRepo, LikeRepository likeRepo, CommentRepository commentRepo) {
        this.friendshipRepo = friendshipRepo;
        this.likeRepo = likeRepo;
        this.commentRepo = commentRepo;
    }

    /** Aggregates real notifications (pending friend requests, likes and comments received) for the given user, most recent first. */
    public List<NotificationResponse> getNotifications(User user) {
        List<NotificationResponse> result = new ArrayList<>();

        for (Friendship f : friendshipRepo.findByRecipientAndStatus(user, Friendship.Status.PENDING)) {
            User from = f.getRequester();
            result.add(new NotificationResponse(
                    "freq-" + f.getId(),
                    "friend_request",
                    f.getCreatedAt(),
                    from.getUsername(), from.getName(), from.getAvatar(),
                    "enviou-te um pedido de amizade",
                    null, null, null,
                    f.getId()
            ));
        }

        for (Like like : likeRepo.findReceivedByOwner(user.getId())) {
            User from = like.getActor();
            var c = like.getActivity().getCatalogGame();
            result.add(new NotificationResponse(
                    "like-" + like.getId(),
                    "like",
                    like.getCreatedAt(),
                    from.getUsername(), from.getName(), from.getAvatar(),
                    "gostou da tua atividade em " + c.getTitle(),
                    like.getActivity().getId(), c.getId(), c.getTitle(),
                    null
            ));
        }

        for (Comment comment : commentRepo.findReceivedByOwner(user.getId())) {
            User from = comment.getActor();
            var c = comment.getActivity().getCatalogGame();
            result.add(new NotificationResponse(
                    "comment-" + comment.getId(),
                    "comment",
                    comment.getCreatedAt(),
                    from.getUsername(), from.getName(), from.getAvatar(),
                    "comentou na tua atividade em " + c.getTitle(),
                    comment.getActivity().getId(), c.getId(), c.getTitle(),
                    null
            ));
        }

        result.sort(Comparator.comparing(NotificationResponse::at).reversed());
        return result;
    }
}
