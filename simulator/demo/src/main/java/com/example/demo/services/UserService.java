package com.example.demo.services;


import com.example.demo.dtos.UserDTO;
import com.example.demo.dtos.UserDetailsDTO;
import com.example.demo.dtos.builders.UserBuilder;
import com.example.demo.entities.User;
import com.example.demo.handlers.exceptions.model.BadCredentialsException;
import com.example.demo.handlers.exceptions.model.ResourceNotFoundException;
import com.example.demo.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${device.service.url}")
    private String deviceServiceUrl;
    @Value("${auth.service.url}")
    private String authServiceUrl;
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDTO> findUsers() {
        List<User> UserList = userRepository.findAll();
        return UserList.stream()
                .map(UserBuilder::toUserDTO)
                .collect(Collectors.toList());
    }

    public UserDetailsDTO findUserById(UUID id) {
        Optional<User> prosumerOptional = userRepository.findById(id);
        if (!prosumerOptional.isPresent()) {
            LOGGER.error("User with id {} was not found in db", id);
            throw new ResourceNotFoundException(User.class.getSimpleName() + " with id: " + id);
        }
        return UserBuilder.toUserDetailsDTO(prosumerOptional.get());
    }
    @Transactional
    public UserDetailsDTO updateUser(UUID id, UserDetailsDTO userDetailsDTO) {
        User userToUpdate = userRepository.findById(id)
                .orElseThrow(() -> {
                    LOGGER.error("Update failed: User with ID {} not found.", id);
                    return new ResourceNotFoundException("User with id: " + id + " not found.");
                });

        userToUpdate.setName(userDetailsDTO.getName());

        User updatedUser = userRepository.save(userToUpdate);
        LOGGER.debug("Successfully updated user with ID {}.", updatedUser.getId());

        return UserBuilder.toUserDetailsDTO(updatedUser);
    }

    public UUID insert(UserDetailsDTO UserDTO) {
        User user = UserBuilder.toEntity(UserDTO);
        user = userRepository.save(user);
        LOGGER.debug("User with id {} was inserted in db", user.getId());
        return user.getId();
    }
    @Transactional
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User with id: " + userId + " not found.");
        }

        // 1️⃣ Ștergem credentialele din Auth-Service
        try {
            restTemplate.delete(authServiceUrl + "/auth/internal/usercred/" + userId);
            LOGGER.debug("Credentials deleted for user {}", userId);
        } catch (Exception e) {
            LOGGER.error("Could not delete credentials for user {}", userId, e);
            throw new IllegalStateException("Auth-Service unavailable, cannot delete user.");
        }

        // 2️⃣ Ștergem relațiile device-user în Device-Service
        try {
            restTemplate.delete(deviceServiceUrl + "/device/internal/relations/" + userId);
            LOGGER.debug("Device relations deleted for user {}", userId);
        } catch (Exception e) {
            LOGGER.error("Could not delete device relations for user {}", userId, e);
            throw new IllegalStateException("Device-Service unavailable, cannot delete user.");
        }

        // 3️⃣ Ștergem user-ul efectiv
        userRepository.deleteById(userId);
        LOGGER.debug("Successfully deleted user {}", userId);
    }



}
