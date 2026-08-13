package com.pm.javagateway.security;

import com.pm.javagateway.config.AiInferenceGrpcClient;
import com.pm.javagateway.core.StateOrchestrator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class CompanionWebSocketHandler implements WebSocketHandler {

    private final AiInferenceGrpcClient grpcClient;
    private final SanitizationUtility sanitizer;
    private final StateOrchestrator stateOrchestrator;
    private final JwtValidator jwtValidator;

    public CompanionWebSocketHandler(AiInferenceGrpcClient grpcClient,
                                     SanitizationUtility sanitizer,
                                     StateOrchestrator stateOrchestrator,
                                     JwtValidator jwtValidator) {
        this.grpcClient = grpcClient;
        this.sanitizer = sanitizer;
        this.stateOrchestrator = stateOrchestrator;
        this.jwtValidator = jwtValidator;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String jwtToken = org.springframework.web.util.UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
                .build().getQueryParams().getFirst("token");
        String userId = (jwtToken != null) ? jwtValidator.extractUserId(jwtToken) : "anonymous_user";

        Flux<WebSocketMessage> responseFlux = session.receive()
                .flatMap(inboundMessage -> {
                    String rawText = inboundMessage.getPayloadAsText();
                    String cleanText = sanitizer.stripInjectionVectors(rawText);

                    // Trigger the non-blocking database query to fetch the user profile.
                    // The flatMap ensures the Netty thread yields while waiting for PostgreSQL.
                    return stateOrchestrator.buildUserContext(userId, cleanText)
                            .doOnNext(context -> System.out.println("Gateway: DbState - "+ context))
                            .flatMapMany(contextString -> {
                                // Once the DB returns the state, open the gRPC stream to Python.
                                System.out.println("Gateway: Routing to pytorch - "+ cleanText + "''");
                                return grpcClient.streamInference(userId, cleanText, contextString);
                            })
                            .doOnNext(token -> System.out.println("Gateway: token - "+ token))
                            .onErrorResume(throwable -> {
                                System.err.println("[Gateway Error] Pipeline Failure: " + throwable.getMessage());
                                return Flux.just("[System Exception] The AI neural engine is currently offline or unreachable.");
                            });
                })
                .doOnComplete(() -> System.out.println("Gateway: Session - "+ session.getId()))
                .map(session::textMessage);

        return session.send(responseFlux);
    }
}