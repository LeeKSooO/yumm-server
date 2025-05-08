package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.MatchRequestDto;
import com.example.demo.dto.MatchResponseDto;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.MatchingService;

import lombok.RequiredArgsConstructor;


@RestController  
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    // 매칭 요청
    @PostMapping("/wait")
    public ResponseEntity<MatchResponseDto> requestMatching(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody MatchRequestDto matchRequestDto
        ) {            
            Long userId = userDetails.getId();
            int count = matchRequestDto.getCount();

            Long roomId = matchingService.requestMatching(userId, count);

            if (roomId != null) {
                return ResponseEntity.ok(
                    MatchResponseDto.builder()
                        .roomId(roomId)
                        .status("matched")
                        .build()
                );
            } else {
                return ResponseEntity.ok(
                    MatchResponseDto.builder()
                        .roomId(null)
                        .status("waiting")
                        .build()
                );
            }
    }
}
