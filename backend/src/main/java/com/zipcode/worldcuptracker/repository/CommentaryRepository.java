package com.zipcode.worldcuptracker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zipcode.worldcuptracker.model.Commentary;

public interface CommentaryRepository extends JpaRepository<Commentary, Long> {
    Optional<Commentary> findByMatchId(Long matchId);
}
