package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@IdClass(ChatParticipantId.class) // 복합 기본 키 클래스 지정
public class ChatParticipant {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false) // FK 컬럼명 지정
    private ChatRoom chatRoom; // 복합 PK의 일부이자 FK

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // FK 컬럼명 지정
    private User user; // 복합 PK의 일부이자 FK

    @Column(nullable = false)
    private LocalDateTime joinedAt; // 날짜/시간 타입으로 변경

    private LocalDateTime leftAt; // nullable (날짜/시간 타입으로 변경)

    @Column(nullable = false)
    private boolean isCurrentMember = true; // boolean 타입으로 변경, 기본값

    // 편의 메서드
    public void leave() {
        this.leftAt = LocalDateTime.now();
        this.isCurrentMember = false;
    }
}