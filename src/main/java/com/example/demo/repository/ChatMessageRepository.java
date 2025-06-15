package com.example.demo.repository;
import com.example.demo.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 특정 채팅방의 메시지 조회 (페이징 처리 필요)
    List<ChatMessage> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);
}