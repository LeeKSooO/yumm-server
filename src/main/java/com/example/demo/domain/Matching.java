package com.example.demo.domain;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.constant.*;
import com.example.demo.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "matching")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Matching {

    // 엔티티의 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이 요청을 보낸 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 인원수
    @Column(name = "group_size", nullable = false)
    private int groupSize;

    // enum 타입 지역 (SEOUL,BUSAN...)
    @Enumerated(EnumType.STRING)
    @Column(name = "region", nullable = false, length = 50)
    private Region region;

    // enum 타입 음식 (KOREAN,CHINESE...)
    @Enumerated(EnumType.STRING)
    @Column(name = "food", nullable = false, length = 50)
    private Food food;
    // 매칭 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING; // 기본값은 대기중

    // 낙관적 락용 버젼
    @Version
    private Long version;

    // 요청 생성 시각
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "group_id", updatable = false, unique = true)
    private Long groupId;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "condition_id")
    private MatchingCondition condition;

    @ManyToMany
    private List<User> matchedUsers;

    @OneToOne(mappedBy = "matching")
    private ChatRoom chatRoom;
}
