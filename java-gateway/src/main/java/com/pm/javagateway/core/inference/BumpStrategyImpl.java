package com.pm.javagateway.core.inference;

import com.pm.javagateway.repositories.UserRepository;
import com.pm.javagateway.repositories.VectorMemoryRepository;
import com.pm.javagateway.config.AiInferenceGrpcClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.UUID;
import java.time.ZonedDateTime;

@Component
public class BumpStrategyImpl implements InferenceStatergy {

    private final UserRepository userRepository;
    private final VectorMemoryRepository vectorMemoryRepository;
    private final AiInferenceGrpcClient grpcsClient;

    public BumpStrategyImpl(UserRepository userRepository, VectorMemoryRepository vectorMemoryRepository,
            AiInferenceGrpcClient grpcsClient) {
        this.userRepository = userRepository;
        this.vectorMemoryRepository = vectorMemoryRepository;
        this.grpcsClient = grpcsClient;
    }

    @Override
    public String getModeName() {
        return "V1_BUMP";
    }

    @Override
    public Mono<InferenceContext> buildContext(String userId, String userPrompt) {
        UUID id = UUID.fromString(userId);

        Mono<String> profileMono = userRepository.insertUserIfNotExists(id)
                .then(userRepository.updateLastInteraction(id, ZonedDateTime.now()))
                .then(userRepository.findById(id))
                .map(user -> {
                    String twin = (user.getDigitalTwinProfile() != null && !user.getDigitalTwinProfile().isEmpty())
                            ? user.getDigitalTwinProfile()
                            : "No Profile yet. ";
                    return "TimeZone: " + user.getTimezone() + " | BUMP profile: " + twin + " | ";
                })
                .defaultIfEmpty("Anonymous User |");

        Mono<String> memoryMono = grpcsClient.generateEmbedding(userPrompt)
                .flatMapMany(vectorString -> vectorMemoryRepository.findTop3SimilarMemories(id, vectorString))
                .map(memory -> "Q: " + memory.getPrompt() + " A: " + memory.getResponse())
                .collectList()
                .map(list -> {
                    if (list.isEmpty()) {
                        return "No prior memory";
                    }
                    return "Past Context: " + String.join(" | ", list);
                });

        return Mono.zip(profileMono, memoryMono,
                (profile, memory) -> new InferenceContext("System Context => " + profile + memory, null) // Action is
                                                                                                         // NULL for V1
        );
    }
}
