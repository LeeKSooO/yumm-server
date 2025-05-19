package com.example.demo.dto;

import com.example.demo.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoResponseDto {

    private String username;
    private String name;
    private String phone;
    private String email;

     public static UserInfoResponseDto from(User user) {
        return UserInfoResponseDto.builder()
            .username(user.getUsername())
            .name(user.getName())
            .phone(user.getPhone())
            .email(user.getEmail())
            .build();
     }
}