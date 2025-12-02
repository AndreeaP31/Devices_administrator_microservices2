package com.example.demo.services;

import com.example.demo.dtos.*;
import com.example.demo.entities.Credential;
import com.example.demo.repositories.CredentialRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${user.service.url}")
    private String userServiceUrl;

    public AuthService(
            CredentialRepository credentialRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // -----------------------------
    // REGISTER – SAGA ORCHESTRATION
    // -----------------------------
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {

        if (credentialRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // 1) Creează user în user-service
        UserDTO userRequest = new UserDTO();
        userRequest.setName(request.getName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UserDTO> httpEntity = new HttpEntity<>(userRequest, headers);

        UserDTO createdUser;

        try {
            createdUser = restTemplate.postForObject(
                    userServiceUrl + "/users",
                    httpEntity,
                    UserDTO.class
            );
        } catch (Exception e) {
            LOGGER.error("User-Service unavailable", e);
            throw new IllegalStateException("Could not create user in User Service");
        }

        if (createdUser == null || createdUser.getId() == null) {
            throw new IllegalStateException("User service returned invalid response");
        }

        Credential credential = new Credential(
                createdUser.getId(),
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getRoleAsEnum()
        );

        try {
            credentialRepository.save(credential);
        } catch (Exception e) {
            LOGGER.error("Failed to save credential, rolling back user creation...", e);
            try {
                restTemplate.delete(userServiceUrl + "/users/" + createdUser.getId());
            } catch (Exception ex) {
                LOGGER.error("SAGA ROLLBACK FAILED! Inconsistent state.", ex);
            }
            throw new IllegalStateException("Could not save credential");
        }

        // 3) Generează token
        String token = jwtService.generateToken(
                credential.getUserId(),
                credential.getUsername(),
                credential.getRole()
        );

        return new AuthResponseDTO(
                token,
                credential.getUserId(),
                credential.getUsername(),
                credential.getRole()
        );
    }

    // -----------------------------
    // LOGIN
    // -----------------------------
    public List<CredentialInfoDTO> getAllCredentials() {
        return credentialRepository.findAll()
                .stream()
                .map(cred -> new CredentialInfoDTO(
                        cred.getUserId(),
                        cred.getUsername(),
                        cred.getRole()
                ))
                .toList();
    }


    public AuthResponseDTO login(LoginRequestDTO request) {

        Optional<Credential> credentialOpt =
                credentialRepository.findByUsername(request.getUsername());

        if (credentialOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        Credential credential = credentialOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtService.generateToken(
                credential.getUserId(),
                credential.getUsername(),
                credential.getRole()
        );

        return new AuthResponseDTO(
                token,
                credential.getUserId(),
                credential.getUsername(),
                credential.getRole()
        );
    }

    // -----------------------------
    // TOKEN HELPERS
    // -----------------------------
    public boolean validateToken(String token) {
        return jwtService.validateToken(token);
    }

    public UUID extractUserId(String token) {
        return jwtService.extractUserId(token);
    }

    public String extractUsername(String token) {
        return jwtService.extractUsername(token);
    }

    public String extractRole(String token) {
        return jwtService.extractRole(token);
    }

    // -----------------------------
    // DELETE
    // -----------------------------
    @Transactional
    public void deleteCredentialByUserId(UUID userId) {
        credentialRepository.deleteByUserId(userId);
        LOGGER.info("Credentials removed for user {}", userId);
    }
}
