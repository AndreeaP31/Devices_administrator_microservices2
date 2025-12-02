package com.example.demo.services;


import com.example.demo.dtos.*;
import com.example.demo.dtos.builders.DeviceBuilder;
import com.example.demo.entities.Device;
import com.example.demo.entities.DeviceUserRelation;
import com.example.demo.handlers.exceptions.model.ResourceNotFoundException;
import com.example.demo.repositories.DeviceRepository;
import com.example.demo.repositories.DeviceUserRelationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeviceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceService.class);
    private final DeviceRepository deviceRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${user.service.url}") // Injectează valoarea din proprietăți
    private String userServiceUrl;
    @Autowired
    public DeviceService(DeviceRepository deviceRepository, DeviceUserRelationRepository relationRepository) {
        this.deviceRepository = deviceRepository;
        this.relationRepository = relationRepository;
    }

    public List<DeviceDTO> findDevices() {
        List<Device> deviceList = deviceRepository.findAll();
        return deviceList.stream()
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }

    public DeviceDetailsDTO findDeviceById(UUID id) {
        Optional<Device> prosumerOptional = deviceRepository.findById(id);
        if (!prosumerOptional.isPresent()) {
            LOGGER.error("Device with id {} was not found in db", id);
            throw new ResourceNotFoundException(Device.class.getSimpleName() + " with id: " + id);
        }
        return DeviceBuilder.toDeviceDetailsDTO(prosumerOptional.get());
    }
    public List<DeviceWithUsersDTO> getDevicesWithUsers() {

        List<Device> devices = deviceRepository.findAll();

        return devices.stream().map(device -> {

            // relațiile pentru device
            List<DeviceUserRelation> relations = relationRepository.findByDeviceId(device.getId());

            // extragem userId-urile
            List<UUID> userIds = relations.stream()
                    .map(DeviceUserRelation::getUserId)
                    .toList();

            // apelăm user-service pentru numele userilor
            List<String> userNames = userIds.stream().map(userId -> {
                try {
                    String url = userServiceUrl + "/users/" + userId;
                    Map user = restTemplate.getForObject(url, Map.class);

                    if (user != null && user.get("name") != null) {
                        return user.get("name").toString();
                    }
                    return "(unknown)";
                } catch (Exception e) {
                    return "(unknown)";
                }
            }).toList();

            return new DeviceWithUsersDTO(
                    device.getId(),
                    device.getName(),
                    device.getMaxCons(),
                    userNames
            );

        }).toList();
    }

    public List<RelationDTO> getAllRelationsWithNames() {

        List<DeviceUserRelation> relations = relationRepository.findAll();

        return relations.stream().map(relation -> {

            // -------------------------
            // 1. Luăm numele userului
            // -------------------------
            String userName = "(unknown)";
            try {
                String url = userServiceUrl + "/users/" + relation.getUserId();
                Map user = restTemplate.getForObject(url, Map.class);

                if (user != null && user.get("name") != null) {
                    userName = user.get("name").toString();
                }
            } catch (Exception e) {
                userName = "(unknown)";
            }

            // -------------------------
            // 2. Luăm numele device-ului
            // -------------------------
            String deviceName = "(unknown)";
            try {
                Device device = deviceRepository.findById(relation.getDeviceId())
                        .orElse(null);

                if (device != null) {
                    deviceName = device.getName();
                }

            } catch (Exception e) {
                deviceName = "(unknown)";
            }

            return new RelationDTO(
                    relation.getId(),
                    relation.getUserId(),
                    relation.getDeviceId(),
                    userName,
                    deviceName
            );

        }).toList();
    }


    public UUID insert(DeviceDetailsDTO DeviceDTO) {
        Device Device = DeviceBuilder.toEntity(DeviceDTO);
        Device = deviceRepository.save(Device);
        LOGGER.debug("Device with id {} was inserted in db", Device.getId());
        return Device.getId();
    }

    private final DeviceUserRelationRepository relationRepository;


    public UUID assignUserToDevice(DeviceUserRelationDTO dto) {

        String userValidationUrl = userServiceUrl + "/users/" + dto.getUserId();
        try {
            restTemplate.getForEntity(userValidationUrl, String.class);
            LOGGER.info("Validation successful: User with ID {} exists.", dto.getUserId());
        } catch (HttpClientErrorException.NotFound e) {
            LOGGER.error("Validation failed: User with ID {} not found.", dto.getUserId());
            throw new ResourceNotFoundException("User with id: " + dto.getUserId() + " not found.");
        } catch (Exception e) {
            LOGGER.error("Error communicating with User-Service", e);
            throw new IllegalStateException("Could not contact User-Service for validation. Please try again later.");
        }

        if (!deviceRepository.existsById(dto.getDeviceId())) {
            LOGGER.error("Validation failed: Device with ID {} not found.", dto.getDeviceId());
            throw new ResourceNotFoundException("Device with id: " + dto.getDeviceId() + " not found.");
        }

        DeviceUserRelation relation = new DeviceUserRelation(
                dto.getUserId(),
                dto.getDeviceId()
        );

        relation = relationRepository.save(relation);
        LOGGER.debug("Successfully assigned user {} to device {}.", dto.getUserId(), dto.getDeviceId());
        return relation.getId();
    }

    public List<DeviceDTO> findDevicesByUserId(UUID userId) {
        List<DeviceUserRelation> relations = relationRepository.findByUserId(userId);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<UUID> deviceIds = relations.stream()
                .map(DeviceUserRelation::getDeviceId)
                .collect(Collectors.toList());

        List<Device> devices = deviceRepository.findAllById(deviceIds);

        return devices.stream()
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }
    public List<DeviceUserRelation> getDevicesForUser(UUID userId) {
        return relationRepository.findByUserId(userId);
    }

    public List<DeviceUserRelation> getUsersForDevice(UUID deviceId) {
        return relationRepository.findByDeviceId(deviceId);
    }

    public void unassignDevice(UUID deviceId) {
        relationRepository.deleteByDeviceId(deviceId);
    }

    @Transactional
    public void deleteDevice(UUID deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            LOGGER.error("Delete failed: Device with ID {} not found.", deviceId);
            throw new ResourceNotFoundException("Device with id: " + deviceId + " not found.");
        }

        relationRepository.deleteByDeviceId(deviceId);
        LOGGER.debug("Deleted all relations for device ID {}.", deviceId);

        deviceRepository.deleteById(deviceId);
        LOGGER.debug("Successfully deleted device with ID {}.", deviceId);
    }

    public void deleteRelationsForUser(UUID userId) {
        relationRepository.deleteByUserId(userId);
        LOGGER.debug("Deleted all relations for user ID {}.", userId);
    }
    @Transactional
    public DeviceDetailsDTO updateDevice(UUID id, DeviceDetailsDTO deviceDetailsDTO) {
        Device deviceToUpdate = deviceRepository.findById(id)
                .orElseThrow(() -> {
                    LOGGER.error("Update failed: Device with ID {} not found.", id);
                    return new ResourceNotFoundException("Device with id: " + id + " not found.");
                });

        deviceToUpdate.setName(deviceDetailsDTO.getName());
        deviceToUpdate.setMaxCons(deviceDetailsDTO.getMaxCons());

        Device updatedDevice = deviceRepository.save(deviceToUpdate);
        LOGGER.debug("Successfully updated device with ID {}.", updatedDevice.getId());

        return DeviceBuilder.toDeviceDetailsDTO(updatedDevice);
    }


}
