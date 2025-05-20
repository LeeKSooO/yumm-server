package com.example.demo.service;

import com.example.demo.dto.ChangePasswordRequestDto;
import com.example.demo.dto.SignupRequestDto;
import com.example.demo.dto.UpdateUserInfoDto;
import com.example.demo.dto.UserInfoResponseDto;

/**
 * 회원 정보 관련 인터페이스
 * 
 * 회원가입, 내 정보 조회, 내 정보 수정, 비밀번호 변경, 회원탈퇴 기능 정의
 */
public interface UserService {

    /** 회원 가입 : 회원 정보 등록 */
    void signup(SignupRequestDto signupRequestDto);


    /** 회원 정보 조회 */
    UserInfoResponseDto getMyInfoByUsername(String username);


    /** 회원 정보 수정 */
    UserInfoResponseDto updateUserInfo(String username, UpdateUserInfoDto updateUserInfoDto);


     /** 회원 비밀번호 변경 */
    void changePassword(String username, ChangePasswordRequestDto changePasswordRequestDto);


    /** 회원 탈퇴 */
    void withdraw(String username);

}
