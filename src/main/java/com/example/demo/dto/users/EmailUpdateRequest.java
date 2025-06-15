package com.example.demo.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailUpdateRequest {
    @Schema(description = "새 이메일", example = "user@example.com")
    private String newEmail;
    @Schema(description = "현재 비밀번호", example = "password123")
    private String currentPassword;
} 