package com.zipcode.worldcuptracker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zipcode.worldcuptracker.model.DataRefreshLog;

public interface DataRefreshLogRepository extends JpaRepository<DataRefreshLog, Long> {
    Optional<DataRefreshLog> findTopByDataTypeOrderByRefreshedAtDesc(String dataType);
}