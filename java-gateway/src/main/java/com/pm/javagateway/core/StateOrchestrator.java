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

    public Mono<String> buildUserContext(String UserId){
        // parse the String uuid from JWT payload
        UUID id = UUID.fromString(UserId);

        // NONBLOCKING r2DBC repo for the user profile
        return userRepository.findById(id)
                .map(user -> {
                    // Serialize the relaitional state into a prompt-freindly stirng for llm
                    return "System context -> Timezeone: "+ user.getTimezone() + "| Proactive Allowed " + user.getProactiveOptIn();
                })
                // Yeid a safe fallback if the DB return empty
                .defaultIfEmpty("System context -> Anonymous/New User");
    }
}
