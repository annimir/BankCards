package com.example.bankcards.dto;

import com.example.bankcards.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public class UserDTO {

    @Data
    @Builder
    public static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private Role role;
        private boolean enabled;
        private LocalDateTime createdAt;
    }

    @Data
    public static class UpdateUserRequest {
        @Email(message = "Invalid email format")
        private String email;

        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
    }

    @Data
    public static class AdminCreateUserRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank(message = "Email is required")
        @Email
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8)
        private String password;

        private Role role = Role.USER;
    }
}