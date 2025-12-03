package com.example.demo.repositories;

import com.example.demo.entities.HourlyConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HourlyConsumptionRepository extends JpaRepository<HourlyConsumption, UUID> {

    // Caută o înregistrare pentru un device la o anumită oră (timestamp)
    Optional<HourlyConsumption> findByDeviceIdAndTimestamp(UUID deviceId, long timestamp);
    List<HourlyConsumption> findByDeviceIdOrderByTimestampAsc(UUID deviceId);
}