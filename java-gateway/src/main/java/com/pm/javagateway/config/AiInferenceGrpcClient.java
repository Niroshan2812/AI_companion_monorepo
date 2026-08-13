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

    public Flux<String> streamInference(String userId, String sanitizedPrompt, String contextString) {
        // Construct the immutable Protobuf request message using the Builder pattern.
        InferenceRequest request = InferenceRequest.newBuilder()
                .setUserId(userId)
                .setSanitizedPrompt(sanitizedPrompt)
                .setVectorContext(contextString)
                .build();

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
}