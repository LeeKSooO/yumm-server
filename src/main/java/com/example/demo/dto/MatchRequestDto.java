package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchRequestDto {

    // private Long userId;
    // 로그인 유저 ID를 @AuthenticationPrincipal을 통해 서버에서 직접 추출하므로 클라이언트가
    // userId를 전달핧 필요는 없다. 추후에 이 부분에 선호 조건 추가가 필요해 보임.
    private int count;
}