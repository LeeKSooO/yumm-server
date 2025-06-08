package com.example.demo.dto.chatting;

import java.time.LocalDateTime;
import java.util.List;
import com.example.demo.dto.users.ProfileResponse;
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
public class ChatRoomResponse {
    private Long roomId;
    private Long matchId; // nullable
    private String roomName; // 방 이름 (자동 생성 또는 유저 지정)
    private int groupSize;
    private List<ProfileResponse> currentParticipants;
    private LocalDateTime createdAt;
    // ...
}