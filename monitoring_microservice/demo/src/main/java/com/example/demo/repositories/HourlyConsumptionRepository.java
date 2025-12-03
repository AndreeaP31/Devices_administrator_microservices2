package com.example.demo.repositories;

import com.example.demo.entities.HourlyConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HourlyConsumptionRepository extends JpaRepository<HourlyConsumption, UUID> {

    // Găsește consumurile pentru un device, eventual sortate după timp
    List<HourlyConsumption> findByDeviceIdOrderByTimestampAsc(UUID deviceId);

    // Query pentru a verifica dacă există deja o înregistrare pentru o anumită oră și un anumit device
    // timestamp-ul aici va reprezenta ora fixă (ex: 10:00, 11:00)
    @Query("SELECT h FROM HourlyConsumption h WHERE h.deviceId = :deviceId AND h.timestamp = :timestamp")
    HourlyConsumption findByDeviceAndTimestamp(@Param("deviceId") UUID deviceId, @Param("timestamp") long timestamp);
}