package com.example.demo.controller;

import com.example.demo.service.UserService;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.common.ApiResponse;
import com.example.demo.dto.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/user") 
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    
    /**
     * 유저 회원가입
     */
    @PostMapping("/signup")
    @Operation(summary = "사용자 회원가입", description = "회원가입 요청 정보를 받아 새 사용자를 등록합니다.")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupRequestDto signupRequest) {

        userService.signup(signupRequest);

        return ApiResponse.created("회원가입이 성공적으로 완료되었습니다.");
    }
    

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    @Operation(summary = "사용자 정보 조회", description = "현재 로그인한 사용자의 상세 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<UserInfoResponseDto>> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {

        UserInfoResponseDto userInfoResponseDtp = userService.getMyInfoByUsername(userDetails.getUsername());

        return ApiResponse.ok("내 정보 조회 성공", userInfoResponseDtp);
    }

    

    /**
     * 내 정보 수정
     */
    @PutMapping("/me")
    @Operation(summary = "사용자 정보 수정", description = "로그인한 사용자의 프로필 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<UserInfoResponseDto>> updateMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                         @RequestBody UpdateUserInfoDto updateUserInfoDto) {

        UserInfoResponseDto userInfoResponseDto = userService.updateUserInfo(userDetails.getUsername(), updateUserInfoDto);

        return ApiResponse.ok("내 정보 수정 완료", userInfoResponseDto);
    }


    /**
     * 비밀번호 변경
     */
    @PutMapping("/password")
    @Operation(summary = "사용자 비밀번호 변경", description = "현재 비밀번호와 새로운 비밀번호를 입력받아 비밀번호를 변경합니다.")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal CustomUserDetails userDetails, 
                                                            @RequestBody ChangePasswordRequestDto changePasswordRequestDto) {

        userService.changePassword(userDetails.getUsername(), changePasswordRequestDto);

        return ApiResponse.ok("비밀번호가 성공적으로 변경되었습니다.");                           
    }


    /**
     * 회원탈퇴
     */
    @DeleteMapping("/withdraw")
    @Operation(summary = "사용자 회원탈퇴", description = "로그인한 사용자의 계정을 삭제합니다. 이 작업은 되돌릴 수 없습니다.")
    public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {

        userService.withdraw(userDetails.getUsername());

        return ApiResponse.ok("회원 탈퇴가 완료되었습니다");                      
    }

}