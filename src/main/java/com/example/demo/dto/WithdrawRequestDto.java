package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원 탈퇴 요청용 DTO
 * — 비밀번호 확인을 위해 password만 받으면 됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequestDto {
    /**
     * 탈퇴 전 본인 확인용 현재 비밀번호
     */
    private String password;
}
