package com.gamevault.security;

import com.gamevault.model.User;
import com.gamevault.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    public AppUserDetailsService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User u = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado"));
        return new UserPrincipal(u.getId(), u.getUsername(), u.getPassword());
    }
}
