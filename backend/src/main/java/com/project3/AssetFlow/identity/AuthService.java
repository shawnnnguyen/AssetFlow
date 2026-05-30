package com.project3.AssetFlow.identity;

import com.project3.AssetFlow.identity.dto.AuthResponse;
import com.project3.AssetFlow.identity.dto.LoginRequest;
import com.project3.AssetFlow.identity.dto.RefreshRequest;
import com.project3.AssetFlow.identity.dto.RegisterRequest;
import com.project3.AssetFlow.identity.securityConfig.JwtService;
import com.project3.AssetFlow.identity.securityConfig.UserPrincipal;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepo.existsByUsername(request.username()) || userRepo.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setActive(true);

        try {
            userRepo.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already exists");
        }

        String jwtToken = jwtService.generateToken(user.getUsername());
        return new AuthResponse(jwtToken, user.getUsername(), user.getRole().name(), user.getId());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepo.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String jwtToken = jwtService.generateToken(principal.getUsername());
        return new AuthResponse(jwtToken, principal.getUsername(), principal.getRole(), principal.getId());
    }

    public AuthResponse refresh(RefreshRequest request) {
        String username;
        try {
            username = jwtService.extractUsername(request.token());
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));

        String newToken = jwtService.generateToken(user.getUsername());
        return new AuthResponse(newToken, user.getUsername(), user.getRole().name(), user.getId());
    }
}