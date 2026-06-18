package com.zipcode.worldcuptracker.repository;

import com.zipcode.worldcuptracker.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByExternalId(Long externalId);

    List<Team> findByGroupNameIgnoreCase(String groupName);

    List<Team> findAllByOrderByNameAsc();
}
