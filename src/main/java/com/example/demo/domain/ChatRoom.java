package com.example.demo.domain;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
public class ChatRoom {
    @Id @GeneratedValue
    private Long id;
    
    private String roomId; // room id(UUID) < 외부 식별자 용
    private int maxUserCount;

    @ManyToMany
    private List<User> participants; // UserIds

    @OneToOne
    @JoinColumn(name = "matching_id") // 매칭id로 어떤 매칭 결과로 만들어졌는지 추척할때 사용
    private Matching matching;

    @Builder
    public ChatRoom(String roomId, int maxUserCount, List<User> participants) {
        this.roomId = roomId;
        this.maxUserCount = maxUserCount;
        this.participants = participants;
    }

}
