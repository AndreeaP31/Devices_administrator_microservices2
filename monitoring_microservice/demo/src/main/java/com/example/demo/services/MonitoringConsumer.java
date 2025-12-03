package com.example.demo.services;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.dtos.DeviceDTO;
import com.example.demo.dtos.DeviceUserRelationDTO;
import com.example.demo.dtos.MeasurementDTO;
import com.example.demo.entities.MonitoredDevice;
import com.example.demo.entities.Measurement;
import com.example.demo.repositories.MonitoredDeviceRepository;
import com.example.demo.repositories.MeasurementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class MonitoringConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringConsumer.class);

    private final MonitoredDeviceRepository deviceRepository;
    private final MeasurementRepository measurementRepository;
    private final ObjectMapper objectMapper; // Jackson Mapper pentru conversie manuală

    public MonitoringConsumer(MonitoredDeviceRepository deviceRepository,
                              MeasurementRepository measurementRepository,
                              ObjectMapper objectMapper) {
        this.deviceRepository = deviceRepository;
        this.measurementRepository = measurementRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.MONITORING_QUEUE)
    public void consumeMessage(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        byte[] body = message.getBody();

        try {
            if (routingKey.startsWith("device.created") || routingKey.startsWith("device.updated")) {
                // 1. Device nou sau modificat
                DeviceDTO dto = objectMapper.readValue(body, DeviceDTO.class);
                handleDeviceUpsert(dto);

            } else if (routingKey.startsWith("device.assigned")) {
                // 2. Device asignat unui user
                DeviceUserRelationDTO dto = objectMapper.readValue(body, DeviceUserRelationDTO.class);
                handleDeviceAssign(dto);

            } else if (routingKey.startsWith("device.deleted")) {
                // 3. Device șters (primim doar UUID)
                UUID deviceId = objectMapper.readValue(body, UUID.class);
                handleDeviceDelete(deviceId);

            } else if (routingKey.startsWith("sensor.measurement")) {
                // 4. Date de la senzor
                MeasurementDTO dto = objectMapper.readValue(body, MeasurementDTO.class);
                handleSensorData(dto);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to process message with key {}: {}", routingKey, e.getMessage());
        }
    }

    private void handleDeviceUpsert(DeviceDTO dto) {
        MonitoredDevice device = new MonitoredDevice(
                dto.getId(),
                dto.getMaxCons(),
                dto.getUserId()
        );
        deviceRepository.save(device);
        LOGGER.info("Synced device: {}", dto.getId());
    }

    private void handleDeviceAssign(DeviceUserRelationDTO dto) {
        Optional<MonitoredDevice> deviceOpt = deviceRepository.findById(dto.getDeviceId());
        if (deviceOpt.isPresent()) {
            MonitoredDevice device = deviceOpt.get();
            device.setUserId(dto.getUserId());
            deviceRepository.save(device);
            LOGGER.info("Assigned device {} to user {}", dto.getDeviceId(), dto.getUserId());
        } else {
            LOGGER.warn("Received assignment for unknown device: {}", dto.getDeviceId());
        }
    }

    private void handleDeviceDelete(UUID deviceId) {
        if (deviceRepository.existsById(deviceId)) {
            deviceRepository.deleteById(deviceId);
            LOGGER.info("Deleted monitored device: {}", deviceId);
        }
    }

    private void handleSensorData(MeasurementDTO dto) {
        LOGGER.info("Received measurement: {} for device {}", dto.getMeasurementValue(), dto.getDeviceId());

        // 1. Salvăm măsurătoarea brută (opțional, dar util pt istoric)
        Measurement measurement = new Measurement(
                dto.getTimestamp(),
                dto.getDeviceId(),
                dto.getMeasurementValue()
        );
        measurementRepository.save(measurement);

        // 2. AICI va urma logica de calcul orar (Următorul pas)
        // checkHourlyConsumption(dto);
    }
}