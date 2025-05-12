package com.example.demo.controller;

import com.example.demo.constant.Food;
import com.example.demo.constant.Region;
import com.example.demo.constant.RequestStatus;
import com.example.demo.domain.Matching;
import com.example.demo.dto.MatchRequestDto;
import com.example.demo.dto.MatchResponseDto;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matchings")
@RequiredArgsConstructor
public class MatchingController {

        private final MatchingService matchingService;

        /* 1) 매칭 요청 생성 */
        @PostMapping("/wait")
        public MatchResponseDto requestMatching(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @RequestBody MatchRequestDto matchRequestDto) {
                return matchingService.createRequest(userDetails.getId(), matchRequestDto);
        }

        /* 2) 매칭 요청 취소 */
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> cancelMatching(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable Long id) {
                matchingService.cancelRequest(userDetails.getId(), id);
                return ResponseEntity.noContent().build();
        }

}
