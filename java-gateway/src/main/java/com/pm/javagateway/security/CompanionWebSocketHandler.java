package com.pm.javagateway.security;

import com.pm.javagateway.config.AiInferenceGrpcClient;
import com.pm.javagateway.core.StateOrchestrator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.pm.javagateway.core.SessionRegistry;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.*;

@Component
public class CompanionWebSocketHandler implements WebSocketHandler {

    private final AiInferenceGrpcClient grpcClient;
    private final SanitizationUtility sanitizer;
    private final StateOrchestrator stateOrchestrator;
    private final JwtValidator jwtValidator;
    private final SessionRegistry sessionRegistry;

    public CompanionWebSocketHandler(AiInferenceGrpcClient grpcClient,
            SanitizationUtility sanitizer,
            StateOrchestrator stateOrchestrator,
            JwtValidator jwtValidator,
            SessionRegistry sessionRegistry) {
        this.grpcClient = grpcClient;
        this.sanitizer = sanitizer;
        this.stateOrchestrator = stateOrchestrator;
        this.jwtValidator = jwtValidator;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String jwtToken = UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
                .build().getQueryParams().getFirst("token");
        String userId = (jwtToken != null) ? jwtValidator.extractUserId(jwtToken) : "anonymous_user";

        // create a reactive sink to allow push message from outside the websocket
        // thread
        Sinks.Many<String> pushSink = Sinks.many().unicast().onBackpressureBuffer();
        sessionRegistry.registerSession(userId, pushSink);

        Flux<WebSocketMessage> proactiveFlux = pushSink.asFlux().map(session::textMessage);

        Flux<WebSocketMessage> responseFlux = session.receive()
                .flatMap(inboundMessage -> {
                    String rawText = inboundMessage.getPayloadAsText();
                    String cleanText = sanitizer.stripInjectionVectors(rawText);

                    StringBuilder fullResponse = new StringBuilder();

                    // Trigger the non-blocking database query to fetch the user profile.
                    // The flatMap ensures the Netty thread yields while waiting for PostgreSQL.
                    return stateOrchestrator.buildUserContext(userId, cleanText)
                            // Updated to extract the system prompt context from the
                            // returned object for logging.
                            .doOnNext(context -> System.out
                                    .println("Gateway: DbState - " + context.getSystemPromptContext()))
                            .flatMapMany(context -> {
                                // Once the DB returns the state, open the gRPC stream to Python.
                                System.out.println("Gateway: Routing to pytorch - " + cleanText + "''");
                                // Updated the call to grpcClient.streamInference to pass
                                // both the system context and the RL action extracted from the context object.
                                return grpcClient.streamInference(userId, cleanText, context.getSystemPromptContext(),
                                        context.getRlAction());
                            })
                            .doOnNext(token -> {
                                System.out.println("Gateway: token - " + token);
                                fullResponse.append(token);
                            })
                            .doOnComplete(() -> {
                                if (fullResponse.length() > 0) {
                                    stateOrchestrator.saveConversationTurn(userId, cleanText, fullResponse.toString())
                                            .subscribe(
                                                    null,
                                                    err -> System.err
                                                            .println("Failed to save memory: " + err.getMessage()));
                                }
                            })
                            .onErrorResume(throwable -> {
                                System.err.println("[Gateway Error] Pipeline Failure: " + throwable.getMessage());
                                return Flux.just(
                                        "[System Exception] The AI neural engine is currently offline or unreachable.");
                            });
                })
                .doOnComplete(() -> System.out.println("Gateway: Session - " + session.getId()))
                .map(session::textMessage);

        return session.send(Flux.merge(responseFlux, proactiveFlux)
                .doFinally(signalType -> sessionRegistry.unregisterSession(userId)));
    }
}