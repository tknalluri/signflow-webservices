package com.app.signflow.service;

import com.app.signflow.config.JwtUtil;
import com.app.signflow.model.dto.*;
import com.app.signflow.model.entity.User;
import com.app.signflow.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .user(convertToDTO(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cannot find Email"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is not correct");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .user(convertToDTO(user))
                .build();
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate reset token (simplified - in production use a proper reset token)
        String resetToken = jwtUtil.generateToken(email);

        // Send email
        emailService.sendPasswordResetEmail(email, resetToken);
    }

        public void changePassword(ChangePasswordRequest request) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
                }

                if (request.getCurrentPassword() == null || request.getNewPassword() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing password fields");
                }

                User user = userRepository.findByEmail(authentication.getName())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
                }

                user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
                userRepository.save(user);
        }

    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .signaturePath(user.getSignaturePath())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
