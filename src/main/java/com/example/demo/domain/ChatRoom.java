package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 채팅방 엔티티
 * 매칭된 사용자들 간의 채팅을 위한 방을 관리
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;  // 채팅방 고유 식별자

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = true, unique = true)
    private MatchingResult matchingResult;  // 매칭 결과와 1:1 관계 (매칭으로 생성된 채팅방인 경우)

    @Column(nullable = false)
    private LocalDateTime createdAt;  // 채팅방 생성 시간

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean isActive = true;  // 채팅방 활성화 상태 (false면 채팅방 종료)

    @Column(nullable = false)
    private int groupSize;  // 채팅방 인원 수 (기본값 2)

    // 채팅방 이름 (선택적 추가)
    @Column(nullable = false)
    private String roomName;

    // 채팅방 설명 (선택적 추가)
    private String description;

    @ManyToMany
    @JoinTable(
        name = "chat_room_members",
        joinColumns = @JoinColumn(name = "room_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> members = new ArrayList<>();  // 채팅방 멤버들

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();  // 엔티티 생성 시 현재 시간 저장
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 채팅방 비활성화
     * 채팅방을 종료할 때 호출
     */
    public void deactivate() {
        this.isActive = false;
    }

   
}