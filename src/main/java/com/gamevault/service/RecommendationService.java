package com.gamevault.service;

import com.gamevault.dto.RecommendationRequest;
import com.gamevault.dto.RecommendationResponse;
import com.gamevault.model.Recommendation;
import com.gamevault.model.User;
import com.gamevault.repository.RecommendationRepository;
import com.gamevault.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RecommendationService {

    private final RecommendationRepository recRepo;
    private final UserRepository userRepo;

    public RecommendationService(RecommendationRepository recRepo, UserRepository userRepo) {
        this.recRepo  = recRepo;
        this.userRepo = userRepo;
    }

    public List<RecommendationResponse> getReceived(Long userId) {
        return recRepo.findByToUserIdOrderBySentAtDesc(userId)
                .stream().map(RecommendationResponse::from).toList();
    }

    public long countUnread(Long userId) {
        return recRepo.countByToUserIdAndReadFalse(userId);
    }

    @Transactional
    public RecommendationResponse send(Long fromUserId, RecommendationRequest req) {
        User from = findUser(fromUserId);
        User to   = findUser(req.toUserId());

        Recommendation rec = new Recommendation();
        rec.setFromUser(from);
        rec.setToUser(to);
        rec.setGameTitle(req.gameTitle());
        rec.setGameEmoji(req.gameEmoji());
        rec.setGenre(req.genre());
        rec.setPlatform(req.platform());
        rec.setMessage(req.message());
        rec.setSentAt(System.currentTimeMillis());
        rec.setRead(false);

        return RecommendationResponse.from(recRepo.save(rec));
    }

    @Transactional
    public RecommendationResponse markRead(Long userId, Long recId) {
        Recommendation rec = findOwned(userId, recId);
        rec.setRead(true);
        return RecommendationResponse.from(recRepo.save(rec));
    }

    @Transactional
    public void dismiss(Long userId, Long recId) {
        recRepo.delete(findOwned(userId, recId));
    }

    private Recommendation findOwned(Long userId, Long recId) {
        Recommendation rec = recRepo.findById(recId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recomendação não encontrada"));
        if (!rec.getToUser().getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return rec;
    }

    private User findUser(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador não encontrado"));
    }
}
