package com.example.demo.repository;
import com.example.demo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    
    // username으로 회원 검색할 수 있게 메서드 추가
    Optional<User> findByUsername(String username);
    
    // email로 회원 검색
    Optional<User> findByEmail(String email);

    // phone으로 회원 검색
    Optional<User> findByPhone(String phone);

}