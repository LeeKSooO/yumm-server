package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.domain.ChatRoom;
import com.example.demo.domain.User;
import com.example.demo.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    public String createRoom(Long groupId, List<User> matchedUsers) {

        ChatRoom room = new ChatRoom();

        room.setRoomId(UUID.randomUUID().toString());
        room.setParticipants(matchedUsers);
        room.setMaxUserCount(matchedUsers.size());

        System.out.println("[DEBUG] ChatRoom 생성됨 - matchingId=" + groupId);
        System.out.println("Participants:");
        for (User u : matchedUsers) {
            System.out.println(" - userId=" + u.getId());
        }

        chatRoomRepository.save(room);
        System.out.println("[DEBUG] ChatRoom 저장 완료: id=" + room.getId() + ", roomId=" + room.getRoomId());

        return room.getRoomId();
    }
}
