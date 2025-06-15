package com.example.demo.repository;
import com.example.demo.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // 활성 상태의 채팅방 조회
    List<ChatRoom> findByIsActiveTrue();
    // 특정 매칭 결과에 연결된 채팅방 조회
    Optional<ChatRoom> findByMatchingResult(MatchingResult result);
}