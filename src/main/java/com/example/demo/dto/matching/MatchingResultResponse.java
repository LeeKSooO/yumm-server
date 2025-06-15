package com.example.demo.dto.matching;

import java.time.LocalDateTime;
//import java.util.List;
//import com.example.demo.dto.users.ProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingResultResponse {
    private Long matchId;
    private Long requestId; // 어떤 요청에 대한 결과인지
    private String status; // 매칭 성공/실패
    private LocalDateTime matchingTimestamp;
    private Long chatRoomId;
    //private List<ProfileResponse> matchedUsers; // 매칭된 사용자 목록
}