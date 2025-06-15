package com.example.demo.repository;
import com.example.demo.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {
    // 특정 채팅방의 현재 참여자 수 조회 (방 소멸 로직용)
    Long countByChatRoomAndIsCurrentMemberTrue(ChatRoom chatRoom);

    // 특정 채팅방의 참여자 목록 조회
    List<ChatParticipant> findByChatRoom(ChatRoom chatRoom);

    // 특정 사용자가 참여한 채팅방 목록 조회
    List<ChatParticipant> findByUser(User user);

    // 특정 채팅방의 특정 사용자 참여 정보 조회
    Optional<ChatParticipant> findByChatRoomAndUser(ChatRoom chatRoom, User user);
}