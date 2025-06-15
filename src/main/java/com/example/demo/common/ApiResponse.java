package com.example.demo.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.Getter;

/**
 * API 응답을 표준화하기 위한 클래스입니다.
 * 모든 API 응답은 이 클래스를 통해 일관된 형식으로 반환됩니다.
 *
 * @param <T> 응답 데이터의 타입
 */
@Getter
public class ApiResponse<T> {

    private final String    message;
    private final T         data;


    private ApiResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }

    /** 메시지 + 데이터 응답 생성 */
    private static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(message, data);
    }

    /** 메시지만 있는 응답 생성 (data = null) */ 
    private static <T> ApiResponse<T> messageOnly(String message) {
        return new ApiResponse<>(message, null);
    }

    /**
     * 200 OK 응답을 생성합니다.
     * 주어진 메시지와 데이터를 포함한 JSON 본문을 생성합니다.
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(of(message, data));
    }

    /**
     * 200 OK 응답을 생성합니다.
     * 주어진 메시지만 포함하고 데이터는 null로 설정합니다.
     * 주로 단순 성공 알림(비밀번호 변경 성공 등)
     */
    public static ResponseEntity<ApiResponse<Void>> ok(String message) {
        return ResponseEntity.ok(messageOnly(message));
    }

    /**
     * 201 Created 응답을 생성합니다.
     * 주어진 메시지만 포함하고 데이터는 null로 설정합니다.
     * 회원가입, 채팅방 생성 등의 기능에 사용
     */
    public static ResponseEntity<ApiResponse<Void>> created(String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageOnly(message));
    }

    /** 에러 응답 생성 */
    public static <T> ResponseEntity<ApiResponse<T>> error(String message, T data) {
        return ResponseEntity.ok(of(message, data));
    }
}
