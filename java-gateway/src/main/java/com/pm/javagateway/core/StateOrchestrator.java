package com.pm.javagateway.core;

import com.pm.javagateway.repositories.UserRepository;
import com.pm.javagateway.repositories.VectorMemoryRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class StateOrchestrator {

    private final UserRepository userRepository;
    private final VectorMemoryRepository vectorMemoryRepository;

    public StateOrchestrator(UserRepository userRepository,  VectorMemoryRepository vectorMemoryRepository) {
        this.userRepository = userRepository;
        this.vectorMemoryRepository = vectorMemoryRepository;
    }

    public Mono<String> buildUserContext(String UserId, String sanitizedPrompt) {
        // delays execution until the webSocket Flux
        return Mono.defer(() -> {
            try {
                // parse the String uuid from JWT payload
                UUID id = UUID.fromString(UserId);

                // retrive static user preferance
                Mono<String> profileMono = userRepository.findById(id)
                        .map(user -> "Timezone: " + user.getTimezone() + " | ")
                        .defaultIfEmpty("Anonymous User |");

                // placeholder for embedding generation
                // mocked !!!!
                String mockEmbeddingString = "[0.0, 0.0, 0.0]";

                // query the vector DB for embedding generation
                Mono<String> memoryMono = vectorMemoryRepository.findTop3SimilarMemories(id, mockEmbeddingString)
                        .map(memory -> "Q: "+ memory.getPrompt() + "A: "+ memory.getResponse())
                        .collectList()
                        .map(list ->{
                            if(list.isEmpty()){
                                return "No prior memory";

                            }
                            return "Past Context: "+ String.join(" | ", list);
                        });

                   return Mono.zip(profileMono, memoryMono, (profile, memory) ->
                           "System Context => "+ profile + memory );
            } catch (IllegalArgumentException e) {
                return Mono.just("System Context -> Anonymous/New User");
            }

        });
    }
}
