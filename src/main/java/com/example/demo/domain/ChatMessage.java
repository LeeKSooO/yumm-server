package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id") // 컬럼명 명확화
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false) // FK 컬럼명 지정
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false) // FK 컬럼명 지정
    private User sender; // User 엔티티 직접 참조 (sender_id -> sender)

    @Column(nullable = false, columnDefinition = "TEXT") // 메시지 내용이 길 수 있으므로 TEXT
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt; // 날짜/시간 타입으로 변경
}