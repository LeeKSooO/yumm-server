package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.chat.ChatRoomMessage;
import com.example.demo.dto.chat.ChatRoomResponse;
import com.example.demo.domain.ChatRoom;

/**
 * 채팅 관련 비즈니스 로직을 처리하는 서비스 인터페이스
 */
public interface ChatService {


    /**
     * 채팅방 생성
     * @param roomName 채팅방 이름
     * @param email 생성자 이메일
     * @return 생성된 채팅방 정보
     */
    ChatRoomResponse createChatRoom(String roomName, String email);
    
    /**
     * 채팅 메시지를 전송하고 저장
     * @param message 전송할 채팅 메시지
     */
    void sendMessage(ChatRoomMessage message);

    /**
     * 특정 채팅방의 메시지 히스토리를 조회
     * @param roomId 채팅방 ID
     * @param userId 현재 로그인한 사용자 ID
     * @return 채팅 메시지 목록
     */
    List<ChatRoomMessage> getChatHistory(Long roomId, String email);

    /**
     * 채팅 메시지를 저장
     * @param message 저장할 채팅 메시지
     */
    void saveMessage(ChatRoomMessage message);

    /**
     * 특정 유저의 채팅방 목록을 조회
     * @param userId 유저 ID
     * @return 채팅방 목록
     */
    List<ChatRoomResponse> getUserChatRooms(String email);


    /**
     * 채팅방 나가기
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    void exitChatRoom(Long roomId, String email);
    /**
     * 채팅방 생성
     * @param request 채팅방 생성 요청 정보
     * @param username 생성자 사용자 ID
     * @return 생성된 채팅방 정보
     */
    ChatRoomResponse joinChatRoom(Long roomId, String email);
    /**
     * 채팅방 정보 조회
     * @param roomId 채팅방 ID
     * @return 채팅방 정보
     */
    ChatRoomResponse getChatRoomInfo(Long roomId);
    /**
     * 채팅방 이름 변경
     * @param roomId 채팅방 ID
     * @param newRoomName 새 채팅방 이름
     * @return 변경된 채팅방 정보
     */
    ChatRoomResponse updateChatRoomName(Long roomId, String newRoomName);
    /**
     * 채팅방 삭제
     * @param roomId 채팅방 ID
     * @param userId 삭제를 요청한 사용자 ID (채팅방의 소유자여야 함)
     */
    void deleteChatRoom(Long roomId, String email);
    /**
     * 채팅방 멤버 목록 조회
     * @param roomId 채팅방 ID
     * @return 채팅방 멤버 목록
     */
    List<ChatRoomResponse> getChatRoomMembers(Long roomId);
    /**
     * 채팅방 멤버 추가
     * @param roomId 채팅방 ID
     * @param userId 추가할 사용자 ID
     * @return 업데이트된 채팅방 정보
     */
    ChatRoomResponse addChatRoomMember(Long roomId, String email);
    /**
     * 채팅방 멤버 제거
     * @param roomId 채팅방 ID
     * @param userId 제거할 사용자 ID
     * @return 업데이트된 채팅방 정보
     */
    ChatRoomResponse removeChatRoomMember(Long roomId, String email);
    /**
     * 채팅방의 활성화 상태를 변경
     * @param roomId 채팅방 ID
     * @param isActive 활성화 상태 (true: 활성화, false: 비활성화)
     * @return 업데이트된 채팅방 정보
     */
    ChatRoomResponse updateChatRoomActiveStatus(Long roomId, boolean isActive);
    /**
     * 채팅방의 마지막 메시지 ID를 업데이트
     * @param roomId 채팅방 ID
     * @param lastMessageId 마지막 메시지 ID
     */
    void updateLastMessageId(Long roomId, Long lastMessageId);
    /**
     * 채팅방의 마지막 메시지 전송 시간을 업데이트
     * @param roomId 채팅방 ID
     * @param lastMessageTime 마지막 메시지 전송 시간
     */
    void updateLastMessageTime(Long roomId, String lastMessageTime);
    /**
     * 채팅방의 알림 설정을 업데이트
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param isNotificationEnabled 알림 활성화 여부
     * @return 업데이트된 채팅방 정보
     */
    ChatRoomResponse updateChatRoomNotification(Long roomId, String email, boolean isNotificationEnabled);
    /**
     * 채팅방의 읽음 상태를 업데이트
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param isRead 읽음 상태 (true: 읽음, false: 안 읽음)
     * @return 업데이트된 채팅방 정보
     */
    ChatRoomResponse updateChatRoomReadStatus(Long roomId, String email, boolean isRead);
   
} 