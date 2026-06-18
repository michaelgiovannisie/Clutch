package com.zipcode.worldcuptracker.repository;

import com.zipcode.worldcuptracker.model.Commentary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentaryRepository extends JpaRepository<Commentary, Long> {

    Optional<Commentary> findByMatchId(Long matchId);
}
