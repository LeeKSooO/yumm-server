package com.example.demo.repository;
import com.example.demo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
        
    // email로 회원 검색(로그인 ID)
    Optional<User> findByEmail(String email);

    // phone으로 회원 검색
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    // 사용자 ID로 FCM 토큰 조회
    @Query("SELECT u.fcmToken FROM User u WHERE u.id = :userId")
    Optional<String> findFcmTokenById(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.fcmToken = :token WHERE u.id = :userId")
    void updateFcmToken(@Param("userId") Long userId, @Param("token") String token);
}