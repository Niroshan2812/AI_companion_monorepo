package com.pm.javagateway.controllers;

import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pm.javagateway.repositories.UserRepository;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final UserRepository userRepository;

    public SettingsController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/mode")
    public Mono<Void> updateAImode(@RequestParam String userID, @RequestParam String mode) {
        return userRepository.updateAiMode(UUID.fromString(userID), mode).then();
    }
}
