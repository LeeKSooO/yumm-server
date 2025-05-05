package com.example.demo.domain;

// JPA Annotation Import
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;
    
    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Instant expiryDate;
}