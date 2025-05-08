package com.example.demo.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.domain.ChatRoom;
import com.example.demo.domain.User;


public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    // 외부에서 UUID 기반 조회
    Optional<ChatRoom> findByRoomId(String roomId);

    // 특정 유저가 속한 방 모두 조회
    List<ChatRoom> findAllByParticipantsContaining(User user); 
}