package com.example.demo.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChangePasswordRequest {
    @Schema(description = "현재 비밀번호",example = "password123")
    private String oldPassword;
    @Schema(description = "새 비밀번호",example = "password1231")
    private String newPassword;
} 