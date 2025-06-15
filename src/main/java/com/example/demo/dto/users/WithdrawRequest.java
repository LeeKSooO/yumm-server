package com.example.demo.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WithdrawRequest {
    @Schema(description = "사용자 ID", example = "user123")
    private String password;
} 