package com.pm.javagateway.repositories;

import com.pm.javagateway.core.ConversationMemory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface VectorMemoryRepository  extends ReactiveCrudRepository<ConversationMemory, UUID> {
    // Execute KNN search on DB engine then cast the incomming java String
    // to the vector type then order by the <-> L2 distance operator limiting to the
    // top 3 most relevent memories

    @Query("SELECT id, user_id, prompt, response, timestamp FROM conversation_memory " +
            "WHERE user_id = :userId " +
            "ORDER BY embedding <-> :queryEmbedding::vector " +
            "LIMIT 3")
    Flux<ConversationMemory> findTop3SimilarMemories(UUID userId, String queryEmbedding);

}
