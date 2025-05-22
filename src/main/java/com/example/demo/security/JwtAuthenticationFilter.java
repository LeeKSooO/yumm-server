package com.example.demo.security;

import com.example.demo.util.JwtUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import org.springframework.lang.NonNull;

/**
 * @extends OncePerRequestFilter : Spring Security 필터 중 하나
 * 
 * 모든 HTTP 요청이 들어올 때 가장 처음 실행되는 보안 필터
 * JwtAuthenticationFilter는 Spring Security 필터 체인에 등록되기 때문에 모든 요청마다 무조건 실행됨
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * == DI ==
     * JwtUtils : 토큰 파싱, 유효성 검사를 담당하는 유틸 클래스(토큰 관련 예외 처리도 여기에서 담당)
     * CustomUserDetailsService : 사용자 정보를 DB에서 불러올 때 사용
     */
    private final JwtUtils jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }


    /**
     * 모든 HTTP 요청마다 실행되는 인증 처리 메서드
     * filterChain.doFilter() 를 호출해서 다음 필터로 넘기지 않을 시 멈춤
     * 
     * == 이 필터의 주 역할 ==
     *  1. 요청 헤더 분석
     *  2. 토큰 파싱 및 유효성 검증
     *  3. 사용자 인증 정보 생성
     *  4. SecurityContext에 인증 정보 등록
     * 
     * @param HttpServletRequest : 클라이언트가 보낸 HTTP 요청 데이터 객체(URL, 헤더, 파라미터, Body 등)
     * @param HttpServletResponse : 서버가 클라이언트에게 돌려줄 응답을 제어하는 객체(상태코드, 응답 헤더, 바디 등)
     * @param FilterChain : 현재 필터 이후의 다음 필터 또는 Controller로 요청 전달하는 체인
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
                                    throws ServletException, IOException {

        /**
         * JWT 인증 필터 적용 여부 판단
         * 
         * Authorization 헤더에서 JWT 추출
         * ## 클라이언트는 HTTP 요청 헤더에 ** Authorization: Bearer <token> ** 형태로 전달해야함
         * Authorization 헤더가 없거나 "Bearer "로 시작하지 않으면,
         * 인증 불필요한 요청으로 간주하고 필터 그냥 통과시킴
         */
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        /**
         * 요청에 JWT가 존재하므로, 본 필터에서 인증 처리 시작
         */
        System.out.println("[JWT Filter] 필터 실행됨: " + request.getRequestURI());
   
        /**
         *  토큰 유효성 검사 로직
         */
        try {

            /**
             * authHeader에서 'Bearer' 접두어를 제거하고 토큰 본문만 반환
             * 
             * JWT 파싱 라이브러리는 '순수한 토큰 문자열'만 받기 때문에 필요
             * authHeader에 접두어("Bearer") 없을 시 예외(JwtException) 발생
             */
            String token = JwtUtils.extractTokenFrom(authHeader);

            /**
             * 유효성 검사
             * 
             * 검사 항목 : 서명 검증, 만료 시각, 토큰 무결성
             * 유효성 검사 통과 못할 시 예외(JwtException) 발생
             */
            jwtUtil.validation(token);

            /**
             * SecurityContext에 인증 객체 등록 (위의 토큰 유효성 검사 통과 시)
             * 
             * 인증 정보를 담은 UsernamePasswordAuthenticationToken 객체를
             * Spring Security의 SecurityContext에 등록
             * 
             * Principal 에 CustomUserDetails 객체가 들어가고
             * 컨트롤러 등에서 @AuthenticationPrincipal을 통해 인증 유저 객체에 접근 가능
             */
                
            //토큰에서 사용자 이름 추출
            String username = jwtUtil.getUsernameFromToken(token);

            // DB에서 사용자 조회 후, CustomUserDetails 생성 < 예외처리 필요한지 확인하기.
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);

            // 인증 객체 생성(Principal = userDetails, Credential = null, Authorities = 권한 목록)
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 요청 정보 설정(ip, 세션 등)
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 인증 객체를 SecurityContext에 등록 (등록된 사용자는 인증된 사용자로 인식)
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException ex) {

            /**
             * 인증 실패 처리 (토큰이 만료되었거나 위조된 경우)
             *  - 처리 1. 현재 Thread에 바인딩된 인증 객체(Authentication) 제거
             *  - 처리 2. 클라이언트에서 accessToken을 재발급 받을 수 있도록 401 Unauthorized 응답
             */

            /**
             * SecurityContextHolder.clearContext() 상세 설명
             * 
             * 현재 Thread에 바인딩된 인증 객체(Authentication) 제거하여 인증되지 않은 사용자로 간주되도록 설정
             * 즉, 인증 실패 상태로 만들어, 이후 인가 필터에서 접근 차단이 이루어질 수 있게 합니다.
             */
            SecurityContextHolder.clearContext();
            
            /**
             * JwtAuthenticationException 상세 설명
             * 
             * JwtException 발생 시, Spring Security가 인식할 수 있도록 JwtAuthenticationException으로 래핑
             *  - 그냥 JwtException 으로 던지면 Spring Security 내부의 ExceptionTranslationFilter는
             *    이걸 인증 실패로 인식하지 못하기 때문에 (500 Internal Server Error여서 인증 실패로 인식 못함)
             *    "인증 실패" 문제가 발생했다는 것을 명시
             * 
             *  - 401 Unauthorized 응답은 클라이언트가
             *    accessToken 만료 시 refreshToken을 사용하여 자동 재인증할 수 있도록 유도
             * 
             *  - 결과적으로 클라이언트에게 401 Unauthorized 응답 전달
             */ 
            throw new JwtAuthenticationException("Invalid or expired JWT token", ex);
        }
    
        filterChain.doFilter(request, response); // 다음 필터 or 컨트롤러로 넘겨줌
    }
}
