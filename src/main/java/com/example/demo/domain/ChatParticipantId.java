package com.example.demo.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // equals와 hashCode 구현 필수
public class ChatParticipantId implements Serializable {
    private Long chatRoom; // ChatParticipant의 chatRoom 필드명과 일치해야 함
    private Long user;     // ChatParticipant의 user 필드명과 일치해야 함
}