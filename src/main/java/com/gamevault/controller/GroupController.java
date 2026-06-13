package com.gamevault.controller;

import com.gamevault.dto.GroupResponse;
import com.gamevault.model.GroupMember;
import com.gamevault.model.GroupPost;
import com.gamevault.model.User;
import com.gamevault.model.UserGroup;
import com.gamevault.repository.UserGroupRepository;
import com.gamevault.repository.UserRepository;
import com.gamevault.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final UserGroupRepository groupRepo;
    private final UserRepository userRepo;

    public GroupController(UserGroupRepository groupRepo, UserRepository userRepo) {
        this.groupRepo = groupRepo;
        this.userRepo = userRepo;
    }

    /** The groups I belong to. */
    @GetMapping
    public List<GroupResponse> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return groupRepo.findByMember(principal.getId())
                .stream().map(g -> GroupResponse.summary(g, principal.getId())).toList();
    }

    /** Group detail with the post feed — members only. */
    @GetMapping("/{id}")
    public GroupResponse detail(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return GroupResponse.full(findAsMember(principal, id), principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public GroupResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                @RequestBody Map<String, Object> body) {
        String name = body.get("name") != null ? body.get("name").toString().trim() : "";
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome é obrigatório");
        String description = body.get("description") != null ? body.get("description").toString().trim() : null;

        User me = userRepo.findById(principal.getId()).orElseThrow();
        UserGroup g = new UserGroup();
        g.setName(name);
        g.setDescription(description == null || description.isBlank() ? null : description);
        g.setOwner(me);
        g.setCreatedAt(System.currentTimeMillis());
        g = groupRepo.save(g);

        GroupMember owner = new GroupMember();
        owner.setGroup(g);
        owner.setUser(me);
        owner.setJoinedAt(System.currentTimeMillis());
        g.getMembers().add(owner);
        return GroupResponse.full(groupRepo.save(g), principal.getId());
    }

    /** Owner adds a member by username. */
    @PostMapping("/{id}/members")
    @Transactional
    public GroupResponse addMember(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        UserGroup g = findAsMember(principal, id);
        if (!g.getOwner().getId().equals(principal.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o dono pode adicionar membros");
        String username = body.getOrDefault("username", "").trim();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilizador não encontrado"));
        boolean already = g.getMembers().stream().anyMatch(m -> m.getUser().getId().equals(user.getId()));
        if (!already) {
            GroupMember m = new GroupMember();
            m.setGroup(g);
            m.setUser(user);
            m.setJoinedAt(System.currentTimeMillis());
            g.getMembers().add(m);
            g = groupRepo.save(g);
        }
        return GroupResponse.full(g, principal.getId());
    }

    /** Leave the group (owner can't leave — delete instead); owner can also remove others via ?username=. */
    @DeleteMapping("/{id}/members")
    @Transactional
    public GroupResponse removeMember(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long id,
                                      @RequestParam(required = false) String username) {
        UserGroup g = findAsMember(principal, id);
        User me = userRepo.findById(principal.getId()).orElseThrow();
        User target = (username == null || username.isBlank())
                ? me
                : userRepo.findByUsername(username.trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilizador não encontrado"));
        boolean removingOther = !target.getId().equals(me.getId());
        if (removingOther && !g.getOwner().getId().equals(me.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o dono pode remover membros");
        if (target.getId().equals(g.getOwner().getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O dono não pode sair — apaga o grupo");
        g.getMembers().removeIf(m -> m.getUser().getId().equals(target.getId()));
        return GroupResponse.full(groupRepo.save(g), principal.getId());
    }

    @PostMapping("/{id}/posts")
    @Transactional
    public GroupResponse addPost(@AuthenticationPrincipal UserPrincipal principal,
                                 @PathVariable Long id,
                                 @RequestBody Map<String, String> body) {
        UserGroup g = findAsMember(principal, id);
        String message = body.getOrDefault("text", "").trim();
        if (message.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A publicação está vazia");
        GroupPost p = new GroupPost();
        p.setGroup(g);
        p.setAuthor(userRepo.findById(principal.getId()).orElseThrow());
        p.setMessage(message);
        p.setCreatedAt(System.currentTimeMillis());
        g.getPosts().add(p);
        return GroupResponse.full(groupRepo.save(g), principal.getId());
    }

    /** Delete a post — by its author or the group owner. */
    @DeleteMapping("/{id}/posts/{postId}")
    @Transactional
    public GroupResponse deletePost(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long id, @PathVariable Long postId) {
        UserGroup g = findAsMember(principal, id);
        GroupPost post = g.getPosts().stream().filter(p -> p.getId().equals(postId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicação não encontrada"));
        boolean allowed = post.getAuthor().getId().equals(principal.getId())
                || g.getOwner().getId().equals(principal.getId());
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não podes apagar esta publicação");
        g.getPosts().remove(post);
        return GroupResponse.full(groupRepo.save(g), principal.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        UserGroup g = findAsMember(principal, id);
        if (!g.getOwner().getId().equals(principal.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o dono pode apagar o grupo");
        groupRepo.delete(g);
    }

    /** Loads the group and asserts the caller is a member (groups are private). */
    private UserGroup findAsMember(UserPrincipal principal, Long id) {
        UserGroup g = groupRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo não encontrado"));
        boolean member = g.getMembers().stream().anyMatch(m -> m.getUser().getId().equals(principal.getId()));
        if (!member) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este grupo é privado");
        return g;
    }
}
