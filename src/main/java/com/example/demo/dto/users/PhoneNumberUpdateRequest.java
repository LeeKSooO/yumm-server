package com.example.demo.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PhoneNumberUpdateRequest {
    @Schema(description = "새 핸드폰 번호",example = "010-2020-2020")
    private String newPhoneNumber;
    @Schema(description = "현재 비밀번호",example = "password123")
    private String currentPassword;
} 