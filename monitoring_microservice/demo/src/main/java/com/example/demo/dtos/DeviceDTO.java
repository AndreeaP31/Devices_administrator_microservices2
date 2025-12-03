package com.example.demo.dtos;

import java.util.UUID;

public class DeviceDTO {
    private UUID id;
    private String name;
    private Double maxCons;
    private UUID userId; // Poate fi null la creare

    public DeviceDTO() {}

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getMaxCons() { return maxCons; }
    public void setMaxCons(Double maxCons) { this.maxCons = maxCons; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "DeviceDTO{id=" + id + ", maxCons=" + maxCons + ", userId=" + userId + "}";
    }
}