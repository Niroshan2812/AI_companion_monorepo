package com.pm.javagateway.core.inference;

import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class QTableRepository {
    private final DatabaseClient databaseClient;

    public QTableRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<Double> getQvalue(UUID userid, String state, String action) {
        return databaseClient
                .sql("SELECT q_value FROM q_table WHERE user_id = :userId AND state = :state AND action = :action")
                .bind("userId", userid)
                .bind("state", state)
                .bind("action", action)
                .map(row -> row.get("q_value", Double.class))
                .one()
                .defaultIfEmpty(0.0);
    }

    public Mono<Void> upsertQValue(UUID userId, String state, String action, double newQValue) {
        return databaseClient.sql(
                "INSERT INTO q_table (user_id, state, action, q_value) VALUES (:userId, :state, :action, :newQValue) " +
                        "ON CONFLICT (user_id, state, action) DO UPDATE SET q_value = :newQValue, updated_at = CURRENT_TIMESTAMP")
                .bind("userId", userId)
                .bind("state", state)
                .bind("action", action)
                .bind("newQValue", newQValue)
                .then();
    }
}
