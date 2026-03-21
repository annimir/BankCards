package com.example.bankcards.service;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserDTO.UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserDTO.UserResponse getUserById(Long id) {
        log.debug("Fetching user by id={}", id);
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public UserDTO.UserResponse getMyProfile(String username) {
        log.debug("Fetching profile for username='{}'", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toResponse(user);
    }

    @Transactional
    public UserDTO.UserResponse createUser(UserDTO.AdminCreateUserRequest request) {
        log.info("Admin creating user: username='{}', email='{}', role={}",
                request.getUsername(), request.getEmail(), request.getRole());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Create user failed: username '{}' already taken", request.getUsername());
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Create user failed: email '{}' already registered", request.getEmail());
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("User created by admin: id={}, username='{}', role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return toResponse(saved);
    }

    @Transactional
    public UserDTO.UserResponse updateMyProfile(String username, UserDTO.UpdateUserRequest request) {
        log.info("User '{}' updating own profile", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (request.getEmail() != null) {
            if (userRepository.existsByEmail(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
                log.warn("Profile update failed for '{}': email '{}' already in use", username, request.getEmail());
                throw new DuplicateResourceException("Email already in use: " + request.getEmail());
            }
            log.debug("Updating email for user '{}'", username);
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            log.debug("Updating password for user '{}'", username);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User saved = userRepository.save(user);
        log.info("Profile updated successfully for username='{}'", username);
        return toResponse(saved);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user id={}", id);
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
        log.info("User id={} deleted successfully", id);
    }

    @Transactional
    public UserDTO.UserResponse toggleUserEnabled(Long id, boolean enabled) {
        log.info("Setting enabled={} for user id={}", enabled, id);
        User user = findById(id);
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        log.info("User id={} enabled={} applied successfully", id, enabled);
        return toResponse(saved);
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public UserDTO.UserResponse toResponse(User user) {
        return UserDTO.UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}