package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.domain.ChatRoom;
import com.example.demo.dto.chat.ChatRoomMessage;
import com.example.demo.dto.chat.ChatRoomResponse;
import com.example.demo.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 채팅 관련 API를 처리하는 컨트롤러입니다.
 * WebSocket을 통한 실시간 메시지 전송과 채팅방 관리 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@Tag(name = "채팅", description = "채팅 관련 API")
public class ChatController {

    private final ChatService chatService;

    /**
     * 새로운 채팅방을 생성합니다.
     * 
     * @param roomName 생성할 채팅방의 이름
     * @param userDetails 현재 인증된 사용자 정보
     * @return 생성된 채팅방 정보를 포함한 응답
     */
    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/rooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createChatRoom(
        @Parameter(description = "채팅방 이름") @RequestBody String roomName,
        @Parameter(description = "인증된 사용자 정보") @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ChatRoomResponse chatRoom = chatService.createChatRoom(roomName, userDetails.getUsername());
            return ApiResponse.ok("채팅방이 성공적으로 생성되었습니다.", chatRoom);
        } catch (Exception e) {
            log.error("채팅방 생성 실패", e);
            return ApiResponse.error("채팅방 생성 실패", null);
        }
    }

    /**
     * 현재 로그인한 사용자가 참여 중인 채팅방 목록을 조회합니다.
     * 
     * @param userDetails 현재 인증된 사용자 정보
     * @return 사용자가 참여 중인 채팅방 목록을 포함한 응답
     */
    @Operation(summary = "내 채팅방 목록 조회", description = "사용자가 참여 중인 채팅방 목록을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/rooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getMyChatRooms(
            @Parameter(description = "인증된 사용자 정보") @AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<ChatRoomResponse> rooms = chatService.getUserChatRooms(userDetails.getUsername());
            return ApiResponse.ok("채팅방 목록 조회 성공", rooms);
        } catch (Exception e) {
            log.error("채팅방 목록 조회 실패", e);
            return ApiResponse.error("채팅방 목록 조회 실패", null);
        }
    }

    /**
     * 사용자가 채팅방에 참여합니다.
     * 
     * @param roomId 참여할 채팅방의 ID
     * @param userDetails 현재 인증된 사용자 정보
     * @return 채팅방 참여 성공 여부를 포함한 응답
     */
    @Operation(summary = "채팅방 참여", description = "사용자가 채팅방에 참여합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 참여 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/rooms/join/{roomId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> joinChatRoom(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "인증된 사용자 정보") @AuthenticationPrincipal UserDetails userDetails) {
        try {
            chatService.joinChatRoom(roomId, userDetails.getUsername());
            return ApiResponse.ok("채팅방 참여 성공");
        } catch (Exception e) {
            log.error("채팅방 참여 실패", e);
            return ApiResponse.error("채팅방 참여 실패", null);
        }
    }

    /**
     * 특정 채팅방의 메시지 히스토리를 조회합니다.
     * 
     * @param roomId 조회할 채팅방의 ID
     * @param userDetails 현재 인증된 사용자 정보
     * @return 채팅방의 메시지 히스토리를 포함한 응답
     */
    @Operation(summary = "채팅 내역 조회", description = "특정 채팅방의 메시지 내역을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/history/{roomId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ChatRoomMessage>>> getChatHistory(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "인증된 사용자 정보") @AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<ChatRoomMessage> messages = chatService.getChatHistory(roomId, userDetails.getUsername());
            return ApiResponse.ok("채팅 내역 조회 성공", messages);
        } catch (Exception e) {
            log.error("채팅 내역 조회 실패", e);
            return ApiResponse.error("채팅 내역 조회 실패", null);
        }
    }

    /**
     * WebSocket을 통해 채팅 메시지를 전송합니다.
     * 클라이언트가 /app/chat/message로 메시지를 보내면 호출됩니다.
     * 
     * @param message 전송할 채팅 메시지 정보
     */
    @Operation(summary = "채팅 메시지 전송", description = "채팅방에 메시지를 전송합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메시지 전송 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @MessageMapping("/chat/message")
    public void sendMessage(@Payload ChatRoomMessage message) {
        try {
            chatService.sendMessage(message);
        } catch (Exception e) {
            log.error("메시지 전송 실패", e);
        }
    }

    /**
     * 사용자가 채팅방을 나갑니다.
     * 
     * @param roomId 나갈 채팅방의 ID
     * @param userDetails 현재 인증된 사용자 정보
     * @return 채팅방 퇴장 성공 여부를 포함한 응답
     */
    @Operation(summary = "채팅방 나가기", description = "사용자가 채팅방을 나갑니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 나가기 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/rooms/exit/{roomId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> exitChatRoom(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "인증된 사용자 정보") @AuthenticationPrincipal UserDetails userDetails) {
        try {
            chatService.exitChatRoom(roomId, userDetails.getUsername());
            return ApiResponse.ok("채팅방 나가기 성공");
        } catch (Exception e) {
            log.error("채팅방 나가기 실패", e);
            return ApiResponse.error("채팅방 나가기 실패", null);
        }
    }


}
