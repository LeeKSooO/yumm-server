package com.example.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController // 이 클래스가 REST API를 처리하는 컨트롤러임을 명시
@RequestMapping("/api/chat") // Common URL Prefix 설정
@RequiredArgsConstructor // final field 자동 주입
public class chatController {
    
}
