package com.pm.javagateway.core;

import com.pm.javagateway.repositories.UserRepository;
import com.pm.javagateway.repositories.VectorMemoryRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.pm.javagateway.config.AiInferenceGrpcClient;

import com.pm.javagateway.core.inference.InferenceContext;
import com.pm.javagateway.core.inference.InferenceStatergy;
import java.util.List;
import java.util.UUID;

@Service
public class StateOrchestrator {

    private final UserRepository userRepository;
    private final VectorMemoryRepository vectorMemoryRepository;
    private final AiInferenceGrpcClient grpcsClient;
    private final List<InferenceStatergy> strategies;

    public StateOrchestrator(UserRepository userRepository, VectorMemoryRepository vectorMemoryRepository,
            AiInferenceGrpcClient grpcsClient, List<InferenceStatergy> strategies) {
        this.userRepository = userRepository;
        this.vectorMemoryRepository = vectorMemoryRepository;
        this.grpcsClient = grpcsClient;
        this.strategies = strategies;
    }

    public Mono<InferenceContext> buildUserContext(String userId, String sanitizedPrompt) {
        UUID id = UUID.fromString(userId);
        // Fetch user's preferred AI mode from DB, then route to the correct Strategy
        return userRepository.findById(id)
                .map(user -> user.getAiMode() != null ? user.getAiMode() : "V1_BUMP")
                .defaultIfEmpty("V1_BUMP")
                .flatMap(mode -> {
                    InferenceStatergy selectedStrategy = strategies.stream()
                            .filter(s -> s.getModeName().equals(mode))
                            .findFirst()
                            .orElse(strategies.get(0));

                    System.out.println("StateOrchestrator: Routing to " + selectedStrategy.getModeName());
                    return selectedStrategy.buildContext(userId, sanitizedPrompt);
                });
    }

    public Mono<Void> saveConversationTurn(String userId, String prompt, String response) {
        UUID id = UUID.fromString(userId);
        return grpcsClient.generateEmbedding(prompt)
                .flatMap(vectorString -> vectorMemoryRepository.saveMemory(id, prompt, response, vectorString))
                .then();
    }

}
