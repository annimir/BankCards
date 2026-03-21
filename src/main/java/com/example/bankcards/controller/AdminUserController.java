package com.example.bankcards.controller;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "Admin user management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<Page<UserDTO.UserResponse>> getAllUsers(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDTO.UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserDTO.UserResponse> createUser(
            @Valid @RequestBody UserDTO.AdminCreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "Enable a user account")
    public ResponseEntity<UserDTO.UserResponse> enableUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserEnabled(id, true));
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "Disable a user account")
    public ResponseEntity<UserDTO.UserResponse> disableUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserEnabled(id, false));
    }
}