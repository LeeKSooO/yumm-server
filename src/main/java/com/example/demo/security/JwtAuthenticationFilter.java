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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
                                    throws ServletException, IOException {

        System.out.println("[JWT Filter] 필터 실행됨: " + request.getRequestURI());
   

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // 유효성 검사 실패하면 JwtException 발생시키거나 false 반환
                if (!jwtUtil.validation(token)) {
                    throw new JwtException("Token expired or invalid");
                }

                //토큰이 유효하면 사용자 이름 추출 후 인증 객체 생성
                String username = jwtUtil.getUsernameFromToken(token);
                CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException ex) {
                // 토큰이 만료되었거나 위조된 경우
                SecurityContextHolder.clearContext();
                // 401 Unauthorized 를 내려주도록 AuthenticationEntryPoint 로 던짐
                throw new JwtAuthenticationException("Invalid or expired JWT token", ex);
            }
        }

        filterChain.doFilter(request, response); // 다음 필터로 넘김
    }
}
