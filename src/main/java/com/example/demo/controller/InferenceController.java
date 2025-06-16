// 추론 서버 연결 테스트용 컨트롤러

package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate; // RestTemplate 사용 예시
import org.springframework.web.reactive.function.client.WebClient; // WebClient 사용 예시
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map; // JSON 데이터를 Map으로 받기 위함

@RestController
public class InferenceController {

    private static final Logger log = LoggerFactory.getLogger(InferenceController.class);
    private final String INFERENCE_SERVER_URL = "http://localhost:8001"; // FastAPI 서버 URL

    // 1. RestTemplate을 사용한 동기 방식 (기존 프로젝트에서 많이 사용)
    @PostMapping("/sendToInferenceSync")
    public String sendDataToInferenceSync(@RequestBody Map<String, Object> requestPayload) {
        log.info("Spring Server: Received request to send data to Inference Server (Sync): {}", requestPayload);

        RestTemplate restTemplate = new RestTemplate();
        String inferenceEndpoint = INFERENCE_SERVER_URL + "/predict"; // FastAPI의 데이터 수신 엔드포인트

        try {
            // FastAPI 서버로 POST 요청 전송
            // requestPayload는 사용자의 임베딩된 매칭 조건들을 담은 Map 형태
            String response = restTemplate.postForObject(inferenceEndpoint, requestPayload, String.class);
            log.info("Spring Server: Received response from Inference Server (Sync): {}", response);
            return "Data sent to inference server (Sync): " + response;
        } catch (Exception e) {
            log.error("Spring Server: Error sending data to Inference Server (Sync): {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    // 2. WebClient를 사용한 비동기 방식 (Spring 5+ 권장, 논블로킹 I/O)
    // WebClient 빈을 주입받아 사용하는 것이 일반적이지만, 예시를 위해 여기서 생성
    @PostMapping("/sendToInferenceAsync")
    public Mono<String> sendDataToInferenceAsync(@RequestBody Map<String, Object> requestPayload) {
        log.info("Spring Server: Received request to send data to Inference Server (Async): {}", requestPayload);

        WebClient webClient = WebClient.builder().baseUrl(INFERENCE_SERVER_URL).build();

        return webClient.post()
                .uri("/predict") // FastAPI의 데이터 수신 엔드포인트
                .bodyValue(requestPayload) // 사용자의 임베딩된 매칭 조건들을 담은 Map 형태
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> log.info("Spring Server: Received response from Inference Server (Async): {}", response))
                .doOnError(error -> log.error("Spring Server: Error sending data to Inference Server (Async): {}", error.getMessage(), error))
                .map(response -> "Data sent to inference server (Async): " + response)
                .onErrorResume(e -> Mono.just("Error: " + e.getMessage()));
    }
}