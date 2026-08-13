package com.pm.javagateway.core;

import com.pm.javagateway.repositories.UserRepository;
import com.pm.javagateway.repositories.VectorMemoryRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.pm.javagateway.config.AiInferenceGrpcClient;

import java.util.UUID;

@Service
public class StateOrchestrator {

    private final UserRepository userRepository;
    private final VectorMemoryRepository vectorMemoryRepository;
    private final AiInferenceGrpcClient grpcsClient;

    public StateOrchestrator(UserRepository userRepository, VectorMemoryRepository vectorMemoryRepository,
            AiInferenceGrpcClient grpcsClient) {
        this.userRepository = userRepository;
        this.vectorMemoryRepository = vectorMemoryRepository;
        this.grpcsClient = grpcsClient;
    }

    public Mono<String> buildUserContext(String UserId, String sanitizedPrompt) {
        UUID id = UUID.fromString(UserId);

        Mono<String> profileMono = userRepository.findById(id)
                .map(user -> "Timezone: " + user.getTimezone() + " | ")
                .defaultIfEmpty("Anonymous User |");

        Mono<String> memoryMono = grpcsClient.generateEmbedding(sanitizedPrompt)
                .flatMapMany(vectorString ->
                // Pass the real generated vector to your incredible pgvector <-> query!
                vectorMemoryRepository.findTop3SimilarMemories(id, vectorString))
                .map(memory -> "Q: " + memory.getPrompt() + " A: " + memory.getResponse())
                .collectList()
                .map(list -> {
                    if (list.isEmpty()) {
                        return "No prior memory";
                    }
                    return "Past Context: " + String.join(" | ", list);
                });

        return Mono.zip(profileMono, memoryMono, (profile, memory) -> "System Context => " + profile + memory);
    }

}
