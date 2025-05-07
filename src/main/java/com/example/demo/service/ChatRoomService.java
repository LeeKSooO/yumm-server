package com.example.demo.service;
import org.springframework.stereotype.Service;

import com.example.demo.domain.ChatRoom;

import java.util.List;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class ChatRoomService {

    private final AtomicLong roomIdCounter = new AtomicLong(1);
    //private final ChatRoom chatRoom;

    public Long createRoom(Long matchingId, List<Long> matchedUsers) {

        // UUID 기반 ChatRoom 생성
        // ChatRoom Entity에 필요한 정보 저장.
        // ChatRoom ID 리턴

        





        // return roomId for test
        Long roomId = roomIdCounter.getAndIncrement();

        return roomId;
    }
}
