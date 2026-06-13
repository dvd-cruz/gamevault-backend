package com.gamevault.service;

import com.gamevault.dto.ActivityResponse;
import com.gamevault.dto.CommentResponse;
import com.gamevault.dto.GameResponse;
import com.gamevault.dto.NotificationResponse;
import com.gamevault.model.Activity;
import com.gamevault.model.Comment;
import com.gamevault.model.Friendship;
import com.gamevault.model.Game;
import com.gamevault.model.GameCatalog;
import com.gamevault.model.Like;
import com.gamevault.model.User;
import com.gamevault.repository.ActivityRepository;
import com.gamevault.repository.CommentRepository;
import com.gamevault.repository.FriendshipRepository;
import com.gamevault.repository.GameCatalogRepository;
import com.gamevault.repository.GameRepository;
import com.gamevault.repository.LikeRepository;
import com.gamevault.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ActivityService {

    /** When unlocking trophies in quick succession for the same game, bundle them into one activity row. */
    private static final long TROPHY_BUNDLE_WINDOW_MS = 1000L * 60 * 60 * 6;

    private final ActivityRepository activityRepo;
    private final FriendshipRepository friendshipRepo;
    private final LikeRepository likeRepo;
    private final CommentRepository commentRepo;
    private final GameCatalogRepository catalogRepo;
    private final GameRepository gameRepo;
    private final UserRepository userRepo;
    private final SseEmitterRegistry sseRegistry;

    public ActivityService(ActivityRepository activityRepo, FriendshipRepository friendshipRepo,
                           LikeRepository likeRepo, CommentRepository commentRepo, GameCatalogRepository catalogRepo,
                           GameRepository gameRepo, UserRepository userRepo, SseEmitterRegistry sseRegistry) {
        this.activityRepo = activityRepo;
        this.friendshipRepo = friendshipRepo;
        this.likeRepo = likeRepo;
        this.commentRepo = commentRepo;
        this.catalogRepo = catalogRepo;
        this.gameRepo = gameRepo;
        this.userRepo = userRepo;
        this.sseRegistry = sseRegistry;
    }

    @Transactional
    public void recordAdded(User actor, GameCatalog catalogGame, String status) {
        record(actor, "added", catalogGame, null, null, null, status);
    }

    @Transactional
    public void recordStatusChange(User actor, GameCatalog catalogGame, String type, Double hours, Integer rating) {
        record(actor, type, catalogGame, null, rating, hours, null);
    }

    /** Unlocking a trophy bundles with any other unlock for the same game within the time window, incrementing its count.
     *  If this exact trophy was already recorded before (even after being re-locked), it is silently ignored. */
    @Transactional
    public void recordTrophyUnlock(User actor, GameCatalog catalogGame, Long trophyId) {
        recordTrophyUnlockBatch(actor, catalogGame, trophyId, 1);
    }

    @Transactional
    public void recordTrophyUnlockBatch(User actor, GameCatalog catalogGame, Long trophyId, int count) {
        // Never record the same trophy twice, regardless of lock/unlock cycles
        if (trophyId != null && activityRepo.existsByActorIdAndCatalogGameIdAndTypeAndTrophyId(actor.getId(), catalogGame.getId(), "trophy", trophyId)) {
            return;
        }
        long now = System.currentTimeMillis();
        var existing = activityRepo.findFirstByActorIdAndCatalogGameIdAndTypeOrderByCreatedAtDesc(actor.getId(), catalogGame.getId(), "trophy");
        if (existing.isPresent() && (now - existing.get().getCreatedAt()) <= TROPHY_BUNDLE_WINDOW_MS) {
            Activity a = existing.get();
            a.setCount((a.getCount() == null ? 0 : a.getCount()) + count);
            a.setCreatedAt(now);
            activityRepo.save(a);
            return;
        }
        Activity a = new Activity();
        a.setActor(actor);
        a.setType("trophy");
        a.setCatalogGame(catalogGame);
        a.setCount(count);
        a.setTrophyId(trophyId);
        a.setCreatedAt(now);
        activityRepo.save(a);
    }

    /** Creates or updates a "review" activity for a game.
     *  If a review already exists for this (actor, game) pair, it is updated in-place
     *  (preserving likes and comments) and marked with editedAt = now. */
    @Transactional
    public void recordReview(User actor, GameCatalog catalogGame, String reviewText, Integer rating, String platform) {
        var existing = activityRepo.findByActorIdAndCatalogGameIdAndType(actor.getId(), catalogGame.getId(), "review");
        if (existing.isPresent()) {
            Activity a = existing.get();
            a.setMessage(reviewText);
            a.setRating(rating);
            a.setPlatform(platform);
            a.setEditedAt(System.currentTimeMillis());
            activityRepo.save(a);
        } else {
            Activity a = new Activity();
            a.setActor(actor);
            a.setType("review");
            a.setCatalogGame(catalogGame);
            a.setRating(rating);
            a.setMessage(reviewText);
            a.setPlatform(platform);
            a.setCreatedAt(System.currentTimeMillis());
            activityRepo.save(a);
        }
    }

    /** Deletes the "review" activity for a (actor, game) pair, if one exists. */
    @Transactional
    public void deleteReview(User actor, GameCatalog catalogGame) {
        activityRepo.findByActorIdAndCatalogGameIdAndType(actor.getId(), catalogGame.getId(), "review")
                .ifPresent(activityRepo::delete);
    }

    /** Creates a "post" activity (text and/or image) on a catalog game's page — visible to friends in their feed. */
    @Transactional
    public ActivityResponse createPost(User actor, Long catalogGameId, String text, String image) {
        String trimmedText = text != null ? text.trim() : null;
        String trimmedImage = image != null ? image.trim() : null;
        if ((trimmedText == null || trimmedText.isBlank()) && (trimmedImage == null || trimmedImage.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A publicação não pode estar vazia");
        }
        GameCatalog catalogGame = catalogRepo.findById(catalogGameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jogo não encontrado no catálogo"));

        Activity a = new Activity();
        a.setActor(actor);
        a.setType("post");
        a.setCatalogGame(catalogGame);
        a.setMessage(blankToNull(trimmedText));
        a.setImage(blankToNull(trimmedImage));
        a.setCreatedAt(System.currentTimeMillis());
        activityRepo.save(a);
        return toResponse(a, actor);
    }

    /** Returns every "publication"-style activity for the given catalog game (posts and reviews — reviews
     *  are a special kind of publication that always carries a star rating), most recent first.
     *  Posts by users with private profiles are filtered out for viewers who aren't their friends. */
    public List<ActivityResponse> getGamePosts(Long catalogGameId, User viewer) {
        return activityRepo.findByCatalogGameIdAndTypeInOrderByCreatedAtDesc(catalogGameId, List.of("post", "review"))
                .stream()
                .filter(a -> canViewPosts(a.getActor(), viewer))
                .map(a -> toResponse(a, viewer)).toList();
    }

    /** Returns every "publication"-style activity authored by the given user (posts, reviews and photos),
     *  most recent first — used for the "As minhas publicações" section on their profile page.
     *  Returns empty list if the author has a private profile and the viewer is not their friend. */
    public List<ActivityResponse> getPostsByUsername(String username, User viewer) {
        User author = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
        if (!canViewPosts(author, viewer)) return List.of();
        return activityRepo.findByActorIdAndTypeInOrderByCreatedAtDesc(author.getId(), List.of("post", "review", "photo"))
                .stream().map(a -> toResponse(a, viewer)).toList();
    }

    /** True if the viewer is allowed to see posts by the author.
     *  Public profiles: always visible. Private profiles: only the author themselves and accepted friends. */
    private boolean canViewPosts(User author, User viewer) {
        if (!author.isPrivateProfile()) return true;
        if (viewer == null) return false;
        if (author.getId().equals(viewer.getId())) return true;
        return friendshipRepo.findBetween(author, viewer)
                .map(f -> f.getStatus() == com.gamevault.model.Friendship.Status.ACCEPTED)
                .orElse(false);
    }

    @Transactional
    public ActivityResponse editPost(User viewer, Long activityId, String text) {
        Activity activity = findActivity(activityId);
        if (!"post".equals(activity.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta atividade não é uma publicação");
        }
        if (!activity.getActor().getId().equals(viewer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não podes editar a publicação de outra pessoa");
        }
        activity.setMessage(text == null || text.isBlank() ? null : text.trim());
        activity.setEditedAt(System.currentTimeMillis());
        return toResponse(activityRepo.save(activity), viewer);
    }

    public void deletePost(User viewer, Long activityId) {
        Activity activity = findActivity(activityId);
        if (!"post".equals(activity.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta atividade não é uma publicação");
        }
        if (!activity.getActor().getId().equals(viewer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não podes apagar a publicação de outra pessoa");
        }
        likeRepo.deleteByActivityId(activityId);
        commentRepo.deleteByActivityId(activityId);
        activityRepo.delete(activity);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private void record(User actor, String type, GameCatalog catalogGame, Integer count, Integer rating, Double hours, String message) {
        Activity a = new Activity();
        a.setActor(actor);
        a.setType(type);
        a.setCatalogGame(catalogGame);
        a.setCount(count);
        a.setRating(rating);
        a.setHours(hours);
        a.setMessage(message);
        a.setCreatedAt(System.currentTimeMillis());
        activityRepo.save(a);
    }

    public ActivityResponse getPost(User viewer, Long activityId) {
        Activity a = findActivity(activityId);
        if (!canViewPosts(a.getActor(), viewer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este perfil é privado");
        }
        return toResponse(a, viewer);
    }

    /** Returns the trophies unlocked by the activity's actor for the activity's game,
     *  within the bundle window ending at the activity's createdAt. */
    public List<GameResponse.GameTrophyResponse> getTrophiesForActivity(Long activityId) {
        Activity a = findActivity(activityId);
        if (!"trophy".equals(a.getType())) return List.of();
        Game game = gameRepo.findByOwnerIdAndCatalogGameId(a.getActor().getId(), a.getCatalogGame().getId())
                .orElse(null);
        if (game == null) return List.of();
        long windowEnd = a.getCreatedAt();
        long windowStart = windowEnd - TROPHY_BUNDLE_WINDOW_MS;
        return game.getUnlockedTrophies().stream()
                .filter(ut -> ut.getMarkedAt() >= windowStart && ut.getMarkedAt() <= windowEnd)
                .map(ut -> GameResponse.GameTrophyResponse.from(ut.getTrophy(), true, ut.getUnlockedAt(), ut.getMarkedAt()))
                .toList();
    }

    /** Returns the activity feed of the given user's friends, most recent first. */
    public List<ActivityResponse> getFriendFeed(User user) {
        List<Friendship> accepted = friendshipRepo.findAcceptedFor(user);
        List<Long> friendIds = accepted.stream()
                .map(f -> f.getRequester().getId().equals(user.getId()) ? f.getRecipient().getId() : f.getRequester().getId())
                .distinct()
                .toList();
        if (friendIds.isEmpty()) return List.of();
        return activityRepo.findByActorIdInOrderByCreatedAtDesc(friendIds)
                .stream().map(a -> toResponse(a, user)).toList();
    }

    /** Community feed: activities from every user with a public profile (not just friends), most recent first. */
    public List<ActivityResponse> getPublicFeed(User user) {
        return activityRepo.findPublicFeed(user.getId())
                .stream().map(a -> toResponse(a, user)).toList();
    }

    private ActivityResponse toResponse(Activity a, User viewer) {
        long likeCount = likeRepo.countByActivityIdAndActiveTrue(a.getId());
        boolean likedByMe = viewer != null && likeRepo.existsByActivityIdAndActorIdAndActiveTrue(a.getId(), viewer.getId());
        List<CommentResponse> comments = commentRepo.findByActivityIdOrderByCreatedAtAsc(a.getId())
                .stream().map(c -> CommentResponse.from(c, viewer != null ? viewer.getId() : null)).toList();
        return ActivityResponse.from(a, likeCount, likedByMe, comments);
    }

    private Activity findActivity(Long activityId) {
        return activityRepo.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atividade não encontrada"));
    }

    /** Toggles the viewer's like on an activity, returning the updated activity (with fresh like/comment info). */
    @Transactional
    public ActivityResponse toggleLike(User viewer, Long activityId) {
        Activity activity = findActivity(activityId);
        var existing = likeRepo.findByActivityIdAndActorId(activityId, viewer.getId());
        // Only ever-first like triggers a notification — re-liking after an unlike stays silent to prevent spam.
        boolean isFirstLikeEver = existing.isEmpty();
        if (existing.isPresent()) {
            Like like = existing.get();
            like.setActive(!like.isActive());
            likeRepo.save(like);
        } else {
            Like like = new Like();
            like.setActor(viewer);
            like.setActivity(activity);
            like.setCreatedAt(System.currentTimeMillis());
            like.setActive(true);
            likeRepo.save(like);
        }
        // Push real-time notification to the activity owner (not if they liked their own)
        User owner = activity.getActor();
        if (isFirstLikeEver && !owner.getId().equals(viewer.getId())) {
            var c = activity.getCatalogGame();
            sseRegistry.push(owner.getId(), new NotificationResponse(
                    "like-" + System.nanoTime(), "like", System.currentTimeMillis(),
                    viewer.getUsername(), viewer.getName(), viewer.getAvatar(),
                    "gostou da tua atividade em " + c.getTitle(),
                    activity.getId(), c.getId(), c.getTitle(), null
            ));
        }
        return toResponse(activity, viewer);
    }

    @Transactional
    public ActivityResponse addComment(User viewer, Long activityId, String text) {
        Activity activity = findActivity(activityId);
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O comentário não pode estar vazio");
        }
        Comment comment = new Comment();
        comment.setActor(viewer);
        comment.setActivity(activity);
        comment.setText(text.trim());
        comment.setCreatedAt(System.currentTimeMillis());
        commentRepo.save(comment);
        // Push real-time notification to the activity owner (not if they commented on their own)
        User owner = activity.getActor();
        if (!owner.getId().equals(viewer.getId())) {
            var c = activity.getCatalogGame();
            sseRegistry.push(owner.getId(), new NotificationResponse(
                    "comment-" + System.nanoTime(), "comment", System.currentTimeMillis(),
                    viewer.getUsername(), viewer.getName(), viewer.getAvatar(),
                    "comentou na tua atividade em " + c.getTitle(),
                    activity.getId(), c.getId(), c.getTitle(), null
            ));
        }
        return toResponse(activity, viewer);
    }

    @Transactional
    public ActivityResponse deleteComment(User viewer, Long activityId, Long commentId) {
        Activity activity = findActivity(activityId);
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentário não encontrado"));
        if (!comment.getActor().getId().equals(viewer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não podes apagar o comentário de outra pessoa");
        }
        commentRepo.delete(comment);
        return toResponse(activity, viewer);
    }
}
