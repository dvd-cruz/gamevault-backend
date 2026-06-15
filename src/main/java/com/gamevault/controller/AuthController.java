package com.gamevault.controller;

import com.gamevault.dto.AuthResponse;
import com.gamevault.dto.LoginRequest;
import com.gamevault.dto.RegisterRequest;
import com.gamevault.dto.UserResponse;
import com.gamevault.model.User;
import com.gamevault.repository.UserRepository;
import com.gamevault.security.JwtService;
import com.gamevault.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepo, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        if (userRepo.findByUsername(req.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome de utilizador já existe");
        }

        User u = new User();
        u.setName(req.name());
        u.setUsername(req.username());
        u.setEmail(req.email());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setJoinedAt(Instant.now().toEpochMilli());
        u = userRepo.save(u);

        UserPrincipal principal = new UserPrincipal(u.getId(), u.getUsername(), u.getPassword());
        String token = jwtService.generateToken(principal);

        return new AuthResponse(token, UserResponse.from(u));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }

        User u = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));

        if (u.isSuspended())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A tua conta foi suspensa por um moderador.");

        UserPrincipal principal = new UserPrincipal(u.getId(), u.getUsername(), u.getPassword());
        String token = jwtService.generateToken(principal);

        return new AuthResponse(token, UserResponse.from(u));
    }
}
