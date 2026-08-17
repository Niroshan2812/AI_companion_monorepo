package com.pm.javagateway.core.inference;

import reactor.core.publisher.Mono;

public interface InferenceStatergy {
    Mono<InferenceContext> buildContext(String userId, String userPrompt);

    String getModeName();

}
