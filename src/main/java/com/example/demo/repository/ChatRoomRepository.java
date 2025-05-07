package com.example.demo.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.domain.ChatRoom;


public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByToken(String token);
    void deleteByUsername(String username);
    
    // 주어진 username 으로 저장된 모든 리프레시 토큰을 조회
    List<ChatRoom> findAllByUsername(String username);
    // 주어진 username 으로 저장된 모든 리프레시 토큰을 한 번에 삭제
    void deleteAllByUsername(String username);
}