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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.Random;

@Service
public class SimulatorService implements CommandLineRunner {

    private final RabbitTemplate rabbitTemplate;
    private final Random random = new Random();
    @Value("${simulator.device-id}")
    private String deviceIdString;


    public SimulatorService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        UUID deviceId;
        try {
            deviceId = UUID.fromString(deviceIdString);
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ INVALID DEVICE ID. Set correct UUID in application.properties or Docker.");
            return;
        }

        System.out.println("🚀 Smart Meter Simulator STARTED for Device: " + deviceId);

        // 1. Initializare Base Load (ex: între 0.5 și 1.5 kWh medie)
        double currentLoad = 0.5 + random.nextDouble();

        // 2. Timpul simulat (pornind de acum)
        long simulatedTime = System.currentTimeMillis();

        while (true) {
            // Aflăm ora curentă din timpul simulat (pentru a aplica tiparul Zi/Noapte)
            LocalDateTime currentTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(simulatedTime), ZoneId.systemDefault());
            int hour = currentTime.getHour();

            // 3. Ajustare în funcție de momentul zilei (Realistic Patterns)
            double timeFactor = 1.0;
            if (hour >= 23 || hour < 7) {
                timeFactor = 0.3; // Noapte: consum mic
            } else if (hour >= 18 && hour < 23) {
                timeFactor = 1.5; // Seara: consum ridicat (Peak)
            }
            // Ziua (07-18) rămâne factor 1.0

            // 4. Calcul valoare finală cu mici fluctuații random (noise)
            double noise = (random.nextDouble() - 0.5) * 0.2; // Fluctuație +/- 0.1
            double measurementValue = (currentLoad * timeFactor) + noise;

            // Asigurăm că nu e negativ
            if (measurementValue < 0) measurementValue = 0.05;

            // 5. Creare Mesaj
            MeasurementDTO measurement = new MeasurementDTO(
                    simulatedTime,
                    deviceId,
                    measurementValue
            );

            // 6. Trimitere la RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    "sensor.measurement",
                    measurement
            );

            System.out.println("📡 Time: " + currentTime.toLocalTime() +
                    " | Val: " + String.format("%.2f", measurementValue) + " kWh");

            // 7. Avansăm timpul și așteptăm
            // Avansăm timpul cu 10 minute pentru următorul punct
            simulatedTime += 10 * 60 * 1000;

            // Așteptăm 2 secunde în timp real (pentru demo, ca să nu aștepți 10 min reale)
            TimeUnit.SECONDS.sleep(2);
        }
    }
}