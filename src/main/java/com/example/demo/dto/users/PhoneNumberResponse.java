package com.example.demo.dto.users;

import com.example.demo.domain.User;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PhoneNumberResponse {
    @Schema(description = "핸드폰 번호",example = "010-1010-1010")
    private final String phoneNumber;

    public static PhoneNumberResponse from(User user) {
        return PhoneNumberResponse.builder()
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
} 