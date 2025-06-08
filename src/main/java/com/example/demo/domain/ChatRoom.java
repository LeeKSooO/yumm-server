package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id") // 컬럼명 명확화
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) // 매칭 결과와 1:1 관계 (Nullable)
    @JoinColumn(name = "match_id", nullable = true, unique = true) // FK 명시, unique=true
    private MatchingResult matchingResult;

    @Column(nullable = false)
    private LocalDateTime createdAt; // 날짜/시간 타입으로 변경

    @Column(nullable = false)
    private boolean isActive; // 채팅방이 활성 상태인지 (인원 0명 시 소멸 처리)

    @Column(nullable = false)
    private int groupSize; // 매칭 그룹 인원수 (초기값 2)

    // 채팅방 이름 (선택적 추가)
    private String roomName;

    // @OneToMany(mappedBy = "chatRoom")
    // private List<ChatParticipant> participants;
    // @OneToMany(mappedBy = "chatRoom")
    // private List<ChatMessage> messages;

    // 편의 메서드 (필요시)
    public void deactivate() {
        this.isActive = false;
    }
}