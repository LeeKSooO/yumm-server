package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponseDto {

    private Long roomId;      // 매칭 완료 시 생성된 채팅방 ID (없을 수도 있음)
    private String status;    // "waiting", "matched", "error" 등 상태 정보
}
