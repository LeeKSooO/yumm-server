package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 인증없이 접근 가능한 URL 패턴 정의
    private static final String[] PUBLIC_URLS = {
        "/api/auth/**",    // 인증 관련 엔드포인트 (로그인, 토큰 재발급 등)
        "/api/user/signup" // 회원가입 엔드포인트
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .parentAuthenticationManager(null)
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 기본 보안 설정
        configureBasicSecurity(http);
        
        // URL 기반 접근 제어 설정
        configureAuthorization(http);
        
        // 예외 처리 설정
        configureExceptionHandling(http);
        
        // JWT 필터 설정
        configureJwtFilter(http);

        return http.build();
    }

    /**
     * 기본적인 보안 설정
     * CSRF 방지 기능을 비활성화, 세션 사용 X (STATELESS)
     * 기본 폼 로그인, HTTP Basic 인증을 비활성화
     *
     * @param http HttpSecurity 인스턴스
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    private void configureBasicSecurity(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());
    }
    
    /**
     * URL별 접근 권한을 설정
     * PUBLIC_URLS에 정의된 경로는 인증 없이 접근을 허용하고,
     * 그 외 모든 요청은 인증된 사용자만 접근할 수 있도록 설정
     *
     * @param http HttpSecurity 인스턴스
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    private void configureAuthorization(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(PUBLIC_URLS).permitAll()
            .anyRequest().authenticated()
        );
    }

    /**
     * 보안 예외 처리
     * 인증되지 않은 사용자가 보호된 리소스에 접근할 경우,
     * HttpStatus.UNAUTHORIZED (401) 응답을 반환
     *
     * @param http HttpSecurity 인스턴스
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    private void configureExceptionHandling(HttpSecurity http) throws Exception {
        http.exceptionHandling(ex -> ex
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        );
    }

    /**
     * 커스텀 JWT 인증 필터를 Spring Security 필터 체인에 등록
     * UsernamePasswordAuthenticationFilter 이전에 실행되도록 설정하여,
     * 요청에 포함된 JWT 토큰 먼저 검증
     *
     * @param http HttpSecurity 인스턴스
     */
    private void configureJwtFilter(HttpSecurity http) {
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}