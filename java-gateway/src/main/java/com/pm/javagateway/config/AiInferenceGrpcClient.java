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

    private ManagedChannel channel;
    private InferenceServiceGrpc.InferenceServiceStub asyncStub;

    @PostConstruct
    public void init() {
        // Initialize the non-blocking gRPC channel targeting the local Python microservice on port 50051.
        //this.channel = ManagedChannelBuilder.forAddress("localhost", 50051)
        this.channel = ManagedChannelBuilder.forAddress("127.0.0.1", 50051)
                .usePlaintext() // Used for local development; will be updated to mTLS SSLContext in production.
                .build();

        // Create the non-blocking gRPC stub required for server-streaming token delivery.
        this.asyncStub = InferenceServiceGrpc.newStub(channel);
    }

    public Flux<String> streamInference(String userId, String sanitizedPrompt) {
        // Construct the immutable Protobuf request message using the Builder pattern.
        InferenceRequest request = InferenceRequest.newBuilder()
                .setUserId(userId)
                .setSanitizedPrompt(sanitizedPrompt)
                .setVectorContext("") // Placeholder for RAG memory context payload in Phase 4.
                .build();

        // Convert the asynchronous gRPC StreamObserver callback pattern into a Reactive Flux stream.
        return Flux.create(sink -> {
            asyncStub.streamTokens(request, new StreamObserver<TokenChunk>() {

                @Override
                public void onNext(TokenChunk chunk) {
                    try {
                        // Check if the Python service issued an end-of-stream signal.
                        if (chunk.getIsComplete()) {
                            sink.complete();
                        } else {
                            //Push the individual generated string token into the reactive pipeline.
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

    @PreDestroy
    public void shutdown() {
        //  close the gRPC channel connection pool during application shutdown.
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}