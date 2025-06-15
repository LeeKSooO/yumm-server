package com.example.demo.dto.users;

import com.example.demo.domain.User;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailResponse {
    @Schema(description = "사용자 이메일",example = "user@example.com")
    private final String email;

    public static EmailResponse from(User user) {
        return EmailResponse.builder()
                .email(user.getEmail())
                .build();
    }
} 