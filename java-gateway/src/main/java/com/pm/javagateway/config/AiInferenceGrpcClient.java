package com.pm.javagateway.config;

import com.companion.grpc.InferenceRequest;
import com.companion.grpc.InferenceServiceGrpc;
import com.companion.grpc.TokenChunk;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import com.companion.grpc.UserProfileRequest;

@Component
public class AiInferenceGrpcClient {
    private InferenceServiceGrpc.InferenceServiceBlockingStub blockingStub;
    private ManagedChannel channel;
    private InferenceServiceGrpc.InferenceServiceStub asyncStub;

    @PostConstruct
    public void init() {
        // Initialize the non-blocking gRPC channel targeting the local Python
        // microservice on port 50051.
        // this.channel = ManagedChannelBuilder.forAddress("localhost", 50051)
        this.channel = ManagedChannelBuilder.forAddress("127.0.0.1", 50051)
                .usePlaintext() // Used for local development; will be updated to mTLS SSLContext in production.
                .build();

        // Create the non-blocking gRPC stub required for server-streaming token
        // delivery.
        this.asyncStub = InferenceServiceGrpc.newStub(channel);
        this.blockingStub = InferenceServiceGrpc.newBlockingStub(channel);
    }

    public Flux<String> streamInference(String userId, String sanitizedPrompt, String contextString, String rlAction) {
        // Construct the immutable Protobuf request message using the Builder pattern.
        InferenceRequest.Builder builder = InferenceRequest.newBuilder()
                .setUserId(userId)
                .setSanitizedPrompt(sanitizedPrompt)
                .setVectorContext(contextString);

        // if v2 q-learning mode attache the action
        if (rlAction != null) {
            builder.setRlAction(rlAction);
        }

        InferenceRequest request = builder.build();

        // Convert the asynchronous gRPC StreamObserver callback pattern into a Reactive
        // Flux stream.
        return Flux.create(sink -> {
            asyncStub.streamTokens(request, new StreamObserver<TokenChunk>() {

                @Override
                public void onNext(TokenChunk chunk) {
                    try {
                        // Check if the Python service issued an end-of-stream signal.
                        if (chunk.getIsComplete()) {
                            sink.complete();
                        } else {
                            // Push the individual generated string token into the reactive pipeline.
                            sink.next(chunk.getToken());
                        }
                    } catch (Exception e) {
                        System.err.println("Exception in onNext: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Throwable t) {
                    // Propagate gRPC connection or execution errors down the reactive stream.
                    sink.error(t);
                }

                @Override
                public void onCompleted() {
                    // Complete the reactive Flux stream upon full response transmission.
                    sink.complete();
                }
            });
        });
    }

    public reactor.core.publisher.Mono<String> generateEmbedding(String text) {
        // wrap the blocking grpc call in a mono so fits into reactive pipline
        return reactor.core.publisher.Mono.fromCallable(() -> {

            com.companion.grpc.EmbeddingRequest request = com.companion.grpc.EmbeddingRequest.newBuilder()
                    .setText(text)
                    .build();

            com.companion.grpc.EmbeddingResponse response = blockingStub.generateEmbedding(request);

            // postgress pgvector expects vector as Strings so try with the java's
            // List.toString
            return response.getEmbeddingList().toString();
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()); // prevent block the netty event loop
    }

    @PreDestroy
    public void shutdown() {
        // close the gRPC channel connection pool during application shutdown.
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public reactor.core.publisher.Mono<String> generateUserProfile(String userId, String rawHistory) {
        UserProfileRequest request = UserProfileRequest.newBuilder()
                .setUserId(userId)
                .setRawInteractionHistory(rawHistory)
                .build();

        return reactor.core.publisher.Mono.create(sink -> {
            asyncStub.generateUserProfile(request,
                    new io.grpc.stub.StreamObserver<com.companion.grpc.UserProfileResponse>() {
                        @Override
                        public void onNext(com.companion.grpc.UserProfileResponse response) {
                            sink.success(response.getCompressedProfile());
                        }

                        @Override
                        public void onError(Throwable t) {
                            sink.error(t);
                        }

                        @Override
                        public void onCompleted() {
                            // handled by onNext
                        }
                    });
        });

    }
}