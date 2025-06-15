package com.example.demo.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지를 표현하는 DTO 클래스
 * WebSocket을 통해 주고받는 메시지의 구조를 정의
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMessage {
    private Long roomId;      // 채팅방 ID
    private String sender;      // 메시지 발신자
    private String message;     // 메시지 내용
    private MessageType type;   // 메시지 타입
    private String timestamp;   // 메시지 전송 시간
    private Boolean isMe;

    /**
     * 메시지 타입을 정의하는 enum
     * - ENTER: 채팅방 입장
     * - TALK: 일반 메시지
     * - LEAVE: 채팅방 퇴장
     */
    public enum MessageType {
        ENTER,  // 채팅방 입장
        TALK,   // 일반 메시지
        LEAVE   // 채팅방 퇴장
    }
} 