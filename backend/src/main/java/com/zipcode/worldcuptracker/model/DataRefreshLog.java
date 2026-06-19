package com.zipcode.worldcuptracker.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class DataRefreshLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String dataType;
    private LocalDateTime refreshedAt;

    public DataRefreshLog() {}

    public DataRefreshLog(String dataType, LocalDateTime refreshedAt) {
        this.dataType = dataType;
        this.refreshedAt = refreshedAt;
    }

    public Long getId() { return id; }
    public String getDataType() { return dataType; }
    public LocalDateTime getRefreshedAt() { return refreshedAt; }

    public void setId(Long id) { this.id = id; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public void setRefreshedAt(LocalDateTime refreshedAt) { this.refreshedAt = refreshedAt; }
}