package com.example.demo.repository;
import com.example.demo.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MatchingResultRepository extends JpaRepository<MatchingResult, Long> {

    // 특정 요청에 대한 매칭 결과 조회
    Optional<MatchingResult> findByMatchingRequestId(Long requestId);    
}
