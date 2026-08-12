package com.pm.javagateway.core;

import com.pm.javagateway.repositories.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class StateOrchestrator {

    private final UserRepository userRepository;

    public StateOrchestrator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<String> buildUserContext(String UserId) {
        // delays execution until the webSocket Flux
        return Mono.defer(() -> {
            try {
                // parse the String uuid from JWT payload
                UUID id = UUID.fromString(UserId);

                return userRepository.findById(id)
                        .map(user -> "System Context -> Timezone: " + user.getTimezone() +
                                " | Proactive Allowed: " + user.getProactiveOptIn())
                        .defaultIfEmpty("System Context -> Anonymous/New User");
            } catch (IllegalArgumentException e) {
                return Mono.just("System Context -> Anonymous/New User");
            }

        });
    }
}
