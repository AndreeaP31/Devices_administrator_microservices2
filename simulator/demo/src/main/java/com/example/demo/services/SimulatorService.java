package com.example.demo.services;

import com.example.demo.RabbitMQConfig;
import com.example.demo.dtos.MeasurementDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SimulatorService implements CommandLineRunner {

    private final RabbitTemplate rabbitTemplate;

    @Value("${simulator.device-id}")
    private String deviceIdString;

    @Value("${simulator.csv-path}")
    private String csvPath;

    public SimulatorService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        UUID deviceId;
        try {
            deviceId = UUID.fromString(deviceIdString);
        } catch (IllegalArgumentException e) {
            System.err.println("INVALID DEVICE ID IN CONFIG. Please set a valid UUID.");
            return;
        }

        System.out.println("🚀 Starting Simulator for Device ID: " + deviceId);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource(csvPath).getInputStream()))) {

            String line;
            while ((line = br.readLine()) != null) {
                try {
                    double value = Double.parseDouble(line.trim());

                    MeasurementDTO measurement = new MeasurementDTO(
                            System.currentTimeMillis(),
                            deviceId,
                            value
                    );

                    // Trimitem mesajul pe Topic
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.EXCHANGE_NAME,
                            "sensor.measurement",
                            measurement
                    );

                    System.out.println("Sent: " + value);

                    // Așteptăm puțin (ex: 3 secunde) pentru a simula trecerea timpului
                    // În realitate ar fi 10 minute, dar pentru demo vrem să vedem datele curgând
                    TimeUnit.SECONDS.sleep(3);

                } catch (NumberFormatException e) {
                    // Ignorăm liniile care nu sunt numere
                }
            }
        }
    }
}