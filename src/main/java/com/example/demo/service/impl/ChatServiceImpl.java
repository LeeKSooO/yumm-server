package com.example.demo.service.impl;

import com.example.demo.service.ChatService;
import com.example.demo.domain.ChatRoom;
import com.example.demo.domain.User;
import com.example.demo.domain.ChatMessage;
import com.example.demo.dto.chat.ChatRoomMessage;
import com.example.demo.dto.chat.ChatRoomResponse;
import com.example.demo.dto.users.ProfileResponse;
import com.example.demo.repository.ChatRoomRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.service.FCMService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 채팅 서비스 구현체
 * WebSocket을 통한 실시간 메시지 전송과 메시지 히스토리 관리를 담당
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    // WebSocket을 통해 클라이언트에게 실시간으로 메시지를 전송하는 템플릿
    private final SimpMessagingTemplate messagingTemplate;
    
    // Redis를 통해 메시지를 발행하고 구독하는 템플릿 (다중 서버 환경에서 메시지 동기화)
    private final RedisTemplate<String, ChatRoomMessage> redisTemplate;
    
    // Redis의 메시지 채널 토픽 (메시지 발행/구독에 사용)
    private final ChannelTopic channelTopic;
    
    // 채팅방 데이터를 저장하고 조회하는 레포지토리
    private final ChatRoomRepository chatRoomRepository;
    
    // 사용자 정보를 저장하고 조회하는 레포지토리
    private final UserRepository userRepository;
    
    // 채팅 메시지를 저장하고 조회하는 레포지토리
    private final ChatMessageRepository chatMessageRepository;
    
    // 메모리 상에서 채팅방별 메시지 히스토리를 임시 저장하는 맵
    private final Map<Long, List<ChatRoomMessage>> chatHistory = new ConcurrentHashMap<>();
    
    // FCM을 통한 푸시 알림 서비스
    @Autowired
    private FCMService fcmService;

    /**
     * 채팅방 생성
     * 1. 새로운 채팅방 생성
     * 2. 사용자 추가
     * 3. 채팅방 저장
     * 4. FCM 푸시 알림 전송
     */
    @Override
    @Transactional
    public ChatRoomResponse createChatRoom(String roomName, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));


        ChatRoom chatRoom = ChatRoom.builder()
            .roomName(roomName)
            .createdAt(LocalDateTime.now())
            .members(new ArrayList<>())
            .build();
        chatRoom.getMembers().add(user);

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        
        // 매칭 성사 시 FCM 알림 전송
        fcmService.sendMatchNotification(
            user.getId(),
            "매칭 성사!",
            "새로운 채팅방이 생성되었습니다."
        );
        
        return ChatRoomResponse.from(savedChatRoom);
    }

    /**
     * 채팅방에 참여
     * 1. 채팅방 존재 여부 확인
     * 2. 사용자가 이미 채팅방에 참여 중인지 확인
     * 3. 채팅방에 사용자 추가
     */
    @Override
    @Transactional
    public ChatRoomResponse joinChatRoom(Long roomId, String email) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        boolean isMember = chatRoom.getMembers().stream()
            .anyMatch(member -> member.getId().equals(user.getId()));

        if (isMember) {
            throw new RuntimeException("이미 참여 중인 채팅방입니다.");
        }


        chatRoom.getMembers().add(user);
        chatRoomRepository.save(chatRoom);

        return ChatRoomResponse.from(chatRoom);
    }

    /**
     * 사용자의 채팅방 목록 조회
     * 1. 사용자 존재 여부 확인
     * 2. 활성화된 채팅방만 필터링
     * 3. 각 채팅방의 정보와 참여자 정보를 DTO로 변환
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getUserChatRooms(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
        return user.getChatRooms().stream()
            .filter(ChatRoom::isActive)
            .map(ChatRoomResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * 채팅방 정보 조회
     * @param roomId 채팅방 ID
     * @return 채팅방 정보
     */
    @Override
    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoomInfo(Long roomId) {
        return chatRoomRepository.findById(roomId)
            .map(ChatRoomResponse::from)
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
    }

    /**
     * 채팅 메시지 전송
     * 1. 채팅방과 사용자 존재 여부 확인
     * 2. 현재 시간을 타임스탬프로 설정
     * 3. Redis를 통해 다른 서버로 메시지 전파
     * 4. WebSocket을 통해 현재 서버의 구독자들에게 메시지 전송
     * 5. DB에 메시지 저장
     * 6. 메모리 캐시에 메시지 저장
     */
    @Override
    @Transactional
    public void sendMessage(ChatRoomMessage chatRoomMessage) {
        try {
            // 채팅방과 사용자 존재 여부 확인
            ChatRoom chatRoom = chatRoomRepository.findById(chatRoomMessage.getRoomId())
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
            
            User sender = userRepository.findByEmail(chatRoomMessage.getSender())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 메시지 타임스탬프 설정
            chatRoomMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // Redis를 통한 메시지 전파
            redisTemplate.convertAndSend(channelTopic.getTopic(), chatRoomMessage);
            
            // WebSocket을 통한 메시지 전송
            messagingTemplate.convertAndSend("/topic/chat/" + chatRoomMessage.getRoomId(), chatRoomMessage);
            
            // DB에 메시지 저장
            ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(chatRoomMessage.getMessage())
                .createdAt(LocalDateTime.now())
                .build();
            
            chatMessageRepository.save(message);
            
            // 메모리 캐시에 메시지 저장
            saveMessage(chatRoomMessage);
            
        } catch (Exception e) {
            throw new RuntimeException("메시지 전송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 채팅방의 메시지 히스토리 조회
     * 1. 채팅방 존재 여부 확인
     * 2. 사용자가 채팅방의 멤버인지 확인
     * 3. DB에서 메시지 조회
     * 4. ChatMessage를 ChatRoomMessage로 변환하여 반환
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomMessage> getChatHistory(Long roomId, String email) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        boolean isMember = chatRoom.getMembers().stream()
            .anyMatch(member -> member.getId().equals(user.getId()));
        
        if (!isMember) {
            throw new AccessDeniedException("해당 채팅방의 멤버가 아닙니다.");
        }

        // DB에서 메시지 조회
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByCreatedAtAsc(chatRoom);
        
        // ChatMessage를 ChatRoomMessage로 변환
        return messages.stream()
            .map(msg -> ChatRoomMessage.builder()
                .roomId(roomId)
                .sender(msg.getSender().getId().toString())
                .message(msg.getContent())
                .type(ChatRoomMessage.MessageType.TALK)
                .timestamp(msg.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build())
            .collect(Collectors.toList());
    }

    /**
     * 메시지를 메모리 캐시에 저장
     * 1. 채팅방 ID를 키로 사용하여 메시지 목록 관리
     * 2. 최근 100개의 메시지만 유지
     */
    @Override
    public void saveMessage(ChatRoomMessage message) {
        synchronized (chatHistory) {
            List<ChatRoomMessage> messages = chatHistory.computeIfAbsent(message.getRoomId(), k -> new ArrayList<>());
            messages.add(message);
            
            // 메시지 히스토리 크기 제한 (최근 100개만 유지)
            if (messages.size() > 100) {
                messages.remove(0);
            }
        }
    }

    /**
     * 채팅방 이름 변경
     * @param roomId 채팅방 ID
     * @param newRoomName 새 채팅방 이름
     * @return 변경된 채팅방 정보
     */
    @Override
    @Transactional
    public ChatRoomResponse updateChatRoomName(Long roomId, String newRoomName) {
        return chatRoomRepository.findById(roomId)
            .map(chatRoom -> {
                chatRoom.setRoomName(newRoomName);
                return ChatRoomResponse.from(chatRoomRepository.save(chatRoom));
            })
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
    }

    /**
     * 채팅방의 알림 설정을 업데이트
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param isNotificationEnabled 알림 활성화 여부
     * @return 업데이트된 채팅방 정보
     */
    @Override
    @Transactional
    public ChatRoomResponse updateChatRoomNotification(Long roomId, String email, boolean isNotificationEnabled) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // TODO: 알림 설정을 저장할 별도의 엔티티나 테이블이 필요할 수 있음
        user.setNotificationEnabled(isNotificationEnabled);
        userRepository.save(user);
        return ChatRoomResponse.from(chatRoom);
    }

    /**
     * 채팅방의 읽음 상태를 업데이트
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param isRead 읽음 상태 (true: 읽음, false: 안 읽음)
     * @return 업데이트된 채팅방 정보
     */
    @Override
    @Transactional
    public ChatRoomResponse updateChatRoomReadStatus(Long roomId, String email, boolean isRead) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        // TODO: 읽음 상태를 저장할 별도의 엔티티나 테이블이 필요할 수 있음
        return ChatRoomResponse.from(chatRoom);
    }

    /**
     * 채팅방 멤버 목록 조회
     * @param roomId 채팅방 ID
     * @return 채팅방 멤버 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getChatRoomMembers(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        return chatRoom.getMembers().stream()
            .map(member -> ChatRoomResponse.builder()
                .id(roomId)
                .currentParticipants(List.of(ProfileResponse.from(member)))
                .build())
            .collect(Collectors.toList());
    }

    /**
     * 채팅방 멤버 추가
     * @param roomId 채팅방 ID
     * @param userId 추가할 사용자 ID
     * @return 업데이트된 채팅방 정보
     */
    @Override
    @Transactional
    public ChatRoomResponse addChatRoomMember(Long roomId, String email) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        boolean isMember = chatRoom.getMembers().stream()
            .anyMatch(member -> member.getId().equals(user.getId()));

        if (isMember) {
            throw new RuntimeException("이미 참여 중인 사용자입니다.");
        }

        chatRoom.getMembers().add(user);
        return ChatRoomResponse.from(chatRoomRepository.save(chatRoom));
    }

    /**
     * 채팅방 멤버 제거
     * @param roomId 채팅방 ID
     * @param userId 제거할 사용자 ID
     * @return 업데이트된 채팅방 정보
     */
    @Override
    @Transactional
    public ChatRoomResponse removeChatRoomMember(Long roomId, String email) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));


        boolean isMember = chatRoom.getMembers().stream()
            .anyMatch(member -> member.getId().equals(user.getId()));

        if (!isMember) {
            throw new AccessDeniedException("해당 채팅방의 멤버가 아닙니다.");
        }

        chatRoom.getMembers().removeIf(member -> member.getId().equals(user.getId()));
        return ChatRoomResponse.from(chatRoomRepository.save(chatRoom));
    }

    /**
     * 채팅방 나가기
     * 1. 채팅방 존재 여부 확인
     * 2. 사용자가 채팅방의 멤버인지 확인
     * 3. 채팅방에서 사용자 제거
     * 4. 퇴장 메시지 전송
     */
    @Override
    @Transactional
    public void exitChatRoom(Long roomId, String email) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
            
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
        // 채팅방에서 사용자 제거
        chatRoom.getMembers().remove(user);
        
        // 퇴장 메시지 전송
        ChatRoomMessage exitMessage = ChatRoomMessage.builder()
            .roomId(roomId)
            .sender(user.getId().toString())
            .message(user.getNickname() + "님이 퇴장하셨습니다.")
            .type(ChatRoomMessage.MessageType.LEAVE)
            .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .build();
            
        sendMessage(exitMessage);
    }

    /**
     * 채팅방 삭제
     * 1. 채팅방 존재 여부 확인
     * 2. 사용자가 채팅방의 소유자인지 확인
     * 3. 채팅방 삭제
     */
    @Override
    @Transactional
    public void deleteChatRoom(Long roomId, String email) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 채팅방의 소유자 확인 (여기서는 단순히 첫 번째 멤버가 소유자라고 가정)
        if (chatRoom.getMembers().isEmpty() || !chatRoom.getMembers().get(0).getId().equals(user.getId())) {
            throw new AccessDeniedException("채팅방 삭제 권한이 없습니다.");
        }

        chatRoomRepository.delete(chatRoom);
    }

    /**
     * 채팅방의 활성화 상태를 변경
     * @param roomId 채팅방 ID
     * @param isActive 활성화 상태 (true: 활성화, false: 비활성화)
     * @return 업데이트된 채팅방 정보
     */
    @Override
    @Transactional
    public ChatRoomResponse updateChatRoomActiveStatus(Long roomId, boolean isActive) {
        return chatRoomRepository.findById(roomId)
            .map(chatRoom -> {
                chatRoom.setActive(isActive);
                return ChatRoomResponse.from(chatRoomRepository.save(chatRoom));
            })
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
    }

    /**
     * 채팅방의 마지막 메시지 ID를 업데이트
     * @param roomId 채팅방 ID
     * @param lastMessageId 마지막 메시지 ID
     */
    @Override
    @Transactional
    public void updateLastMessageId(Long roomId, Long lastMessageId) {
        // 채팅방의 마지막 메시지 ID 업데이트
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        chatRoom.setLastMessageId(lastMessageId);
        chatRoomRepository.save(chatRoom);
    }

    /**
     * 채팅방의 마지막 메시지 전송 시간을 업데이트
     * @param roomId 채팅방 ID
     * @param lastMessageTime 마지막 메시지 전송 시간
     */
    @Override
    @Transactional
    public void updateLastMessageTime(Long roomId, String lastMessageTime) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));
        chatRoom.setLastMessageTime(LocalDateTime.parse(lastMessageTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        chatRoomRepository.save(chatRoom);
    }
} 