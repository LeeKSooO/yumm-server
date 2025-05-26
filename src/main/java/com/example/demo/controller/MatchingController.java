package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.constant.RequestStatus;
import com.example.demo.dto.MatchRequestDto;
import com.example.demo.dto.MatchResponseDto;
import com.example.demo.dto.MatchStatusDto;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.MatchingService;

import io.swagger.v3.oas.annotations.Operation;
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
        @Operation(summary = "매칭 요청 생성", description = "매칭 요청을 생성합니다.")
        public ResponseEntity<ApiResponse<MatchResponseDto>> requestMatching(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @RequestBody MatchRequestDto matchRequestDto) {
                MatchResponseDto matchResponseDto = matchingService.createRequest(userDetails.getId(), matchRequestDto);
                return ApiResponse.ok("매칭이 요청되었습니다.", matchResponseDto);
        }

        /* 2) 매칭 상태 조회 */
        @GetMapping("/status")
        @Operation(summary = "내 매칭 상태 조회", description = "현재 내 매칭 요청 상태(대기·매칭완료·취소)를 조회합니다.")
        public ResponseEntity<ApiResponse<MatchStatusDto>> getMyMatchingStatus(
                        @AuthenticationPrincipal CustomUserDetails userDetails) {
                MatchStatusDto statusDto = matchingService.getRequestStatus(userDetails.getId());
                return ApiResponse.ok("현재 매칭 상태입니다.", statusDto);
        }

        /* 3) 매칭 요청 취소 */
        @DeleteMapping("/{id}")
        @Operation(summary = "매칭 요청 취소", description = "매칭 요청 중 취소를 합니다.")
        public ResponseEntity<ApiResponse<Void>> cancelMatching(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable Long id) {
                matchingService.cancelRequest(userDetails.getId(), id);
                return ApiResponse.ok("매칭 요청이 취소되었습니다");
        }

}
