package com.example.demo.services;

import com.example.demo.dtos.MeasurementDTO;
import com.example.demo.entities.HourlyConsumption;
import com.example.demo.entities.MonitoredDevice;
import com.example.demo.repositories.HourlyConsumptionRepository;
import com.example.demo.repositories.MonitoredDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class MonitoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringService.class);

    private final HourlyConsumptionRepository consumptionRepository;
    private final MonitoredDeviceRepository deviceRepository;

    public MonitoringService(HourlyConsumptionRepository consumptionRepository, MonitoredDeviceRepository deviceRepository) {
        this.consumptionRepository = consumptionRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public void processMeasurement(MeasurementDTO measurementDTO) {
        // 1. Identificăm device-ul pentru a afla limita maximă
        Optional<MonitoredDevice> deviceOpt = deviceRepository.findById(measurementDTO.getDeviceId());

        if (deviceOpt.isEmpty()) {
            LOGGER.warn("Received data for unknown device ID: {}", measurementDTO.getDeviceId());
            return;
        }

        MonitoredDevice device = deviceOpt.get();

        // 2. Calculăm ora curentă (timestamp rotunjit la oră fixă)
        long currentHourTimestamp = getHourTimestamp(measurementDTO.getTimestamp());

        // 3. Căutăm sau creăm înregistrarea de consum pentru această oră
        HourlyConsumption consumption = consumptionRepository
                .findByDeviceIdAndTimestamp(device.getId(), currentHourTimestamp)
                .orElse(new HourlyConsumption(device.getId(), currentHourTimestamp, 0.0));

        // 4. Adăugăm valoarea curentă la total
        double newTotal = consumption.getTotalConsumption() + measurementDTO.getMeasurementValue();
        consumption.setTotalConsumption(newTotal);

        consumptionRepository.save(consumption);
        LOGGER.info("Updated consumption for device {}: {} (+{})", device.getId(), newTotal, measurementDTO.getMeasurementValue());

        // 5. Verificăm depășirea limitei

    }



    private long getHourTimestamp(long timestampInMillis) {
        // Convertim timestamp-ul în LocalDateTime, setăm minute/secunde la 0, apoi înapoi în timestamp
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampInMillis), ZoneId.systemDefault());
        LocalDateTime hourStart = dateTime.truncatedTo(ChronoUnit.HOURS);
        return hourStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}