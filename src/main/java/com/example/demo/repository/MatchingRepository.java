package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Matching;

public interface MatchingRepository extends JpaRepository<Matching,Long> {

}
