package com.pm.javagateway.core;

import com.pm.javagateway.config.AiInferenceGrpcClient;
import com.pm.javagateway.repositories.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
public class ProactiveWorker {

    private final UserRepository userRepository;
    private final StateOrchestrator stateOrchestrator;
    private final AiInferenceGrpcClient grpcClient;
    private final SessionRegistry sessionRegistry;

    public ProactiveWorker(UserRepository userRepository, StateOrchestrator stateOrchestrator,
            AiInferenceGrpcClient grpcClient, SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.stateOrchestrator = stateOrchestrator;
        this.grpcClient = grpcClient;
        this.sessionRegistry = sessionRegistry;
    }

    // Run every 60 seconds (for testing purposes, instead of daily)
    @Scheduled(fixedRate = 60000)
    public void runProactiveEngagement() {
        // Find users inactive for more than 5 minutes (for testing)
        ZonedDateTime threshold = ZonedDateTime.now().minusMinutes(5);

        userRepository.findDormantUsersForProactiveMessaging(threshold)
                .filter(user -> sessionRegistry.isUserConnected(user.getId().toString())) // Only message if they have
                                                                                          // an active socket
                .flatMap(user -> {
                    String userIdStr = user.getId().toString();
                    String proactivePrompt = "The user has been quiet for a while. Send a short, friendly 1-sentence check-in based on our past context.";

                    System.out.println("ProactiveWorker: Engaging dormant user " + userIdStr);

                    // Re-use the existing pipeline to gather context and stream inference
                    return stateOrchestrator.buildUserContext(userIdStr, proactivePrompt)
                            .flatMapMany(context -> grpcClient.streamInference(userIdStr, proactivePrompt, context))
                            .reduce(new StringBuilder(), StringBuilder::append)
                            .map(StringBuilder::toString)
                            .doOnNext(message -> {
                                // Push the generated message directly down the open WebSocket
                                sessionRegistry.pushMessage(userIdStr, "\n\n[Proactive Check-in]: " + message + "\n");
                            })
                            // Save this check-in to Postgres pgvector so the AI remembers reaching out
                            .flatMap(message -> stateOrchestrator.saveConversationTurn(userIdStr,
                                    "SYSTEM PROACTIVE TRIGGER", message))
                            // Update the timestamp so we don't spam them again in 60 seconds
                            .then(stateOrchestrator.updateUserInteractionTimeStamp(userIdStr));
                })
                .subscribe(
                        null,
                        err -> System.err.println("Error in Proactive Worker: " + err.getMessage()));
    }
}
