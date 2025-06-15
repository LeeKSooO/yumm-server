package com.example.demo.dto.chat;

import java.time.LocalDateTime;
import java.util.List;
import com.example.demo.dto.users.ProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.example.demo.domain.ChatRoom;

/**
 * 채팅방 정보 응답 DTO
 * 클라이언트에 전달되는 채팅방의 상세 정보를 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {
    /**
     * 채팅방의 고유 식별자
     */
    private Long id;

    /**
     * 매칭 결과 ID (선택적)
     */
    private Long matchId;

    /**
     * 채팅방 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 채팅방 활성화 여부
     */
    private boolean isActive;

    /**
     * 채팅방의 최대 참여자 수
     */
    private int groupSize;

    /**
     * 채팅방 이름
     */
    private String roomName;

    /**
     * 채팅방 설명
     */
    private String description;

    /**
     * 채팅방에 참여 중인 사용자들의 프로필 정보 목록
     */
    private List<ProfileResponse> currentParticipants;

    /**
     * ChatRoom 엔티티를 ChatRoomResponse DTO로 변환
     * @param chatRoom 변환할 채팅방 엔티티
     * @return ChatRoomResponse DTO
     */
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .createdAt(chatRoom.getCreatedAt())
                .isActive(chatRoom.isActive())
                .groupSize(chatRoom.getGroupSize())
                .roomName(chatRoom.getRoomName())
                .description(chatRoom.getDescription())
                .currentParticipants(chatRoom.getMembers().stream()
                        .map(ProfileResponse::from)
                        .toList())
                .build();
    }
}