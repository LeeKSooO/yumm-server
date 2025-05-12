package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.demo.constant.RequestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponseDto {

    private Long matchId;
    private RequestStatus status; // "waiting", "matched", "error" 등 상태 정보
    private LocalDateTime createdAt;
}
