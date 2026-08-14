package com.pm.javagateway.core;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pm.javagateway.config.AiInferenceGrpcClient;
import com.pm.javagateway.repositories.UserRepository;
import com.pm.javagateway.repositories.VectorMemoryRepository;

@Component
public class UserModelingWorker {
    private final UserRepository userRepository;
    private final VectorMemoryRepository memoryRepository;
    private final AiInferenceGrpcClient grpcClient;

    public UserModelingWorker(UserRepository userRepository, VectorMemoryRepository memoryRepository,
            AiInferenceGrpcClient grpcClient) {
        this.userRepository = userRepository;
        this.memoryRepository = memoryRepository;
        this.grpcClient = grpcClient;
    }

    // Run every 6 hr for testing
    @Scheduled(fixedRate = 21600000)
    public void generateDigitalTwinProfiles() {
        System.out.println("BUMP profiler - Startign backgroung user compression job ");

        // grab all users who opted in
        userRepository.findAll()
                .filter(user -> Boolean.TRUE.equals(user.getProactiveOptIn()))
                .flatMap(user -> {
                    String userIdStr = user.getId().toString();

                    // fetch raw logs
                    return memoryRepository.findRecentMemory(user.getId())
                            .map(mem -> "Q: " + mem.getPrompt() + " | A: " + mem.getResponse())
                            .collectList()
                            .flatMap(logs -> {
                                if (logs.isEmpty())
                                    return reactor.core.publisher.Mono.empty();

                                String rawHistory = String.join("\n", logs);
                                System.out.println("BUMP Profiler -  Generating twin for user " + userIdStr);

                                // send to python LLM for compression
                                return grpcClient.generateUserProfile(userIdStr, rawHistory)
                                        // save new compress backto db
                                        .flatMap(compressProfile -> userRepository
                                                .updateDigitalTwinProfile(user.getId(), compressProfile));

                            });
                })
                .subscribe(
                        success -> {
                        },
                        err -> System.err.println("Error in BUMP profile " + err.getMessage()));
    }

}
