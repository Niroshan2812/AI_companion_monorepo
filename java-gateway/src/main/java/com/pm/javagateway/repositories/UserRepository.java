package com.pm.javagateway.repositories;

import com.pm.javagateway.core.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.UUID;

public interface UserRepository extends ReactiveCrudRepository<User, UUID> {

    // custom reactive query for the corn job to find dormant users
    // the @Quer annotion safely parameterized inputs, prevent SQL injection vectors
    @Query("SELECT * FROM users WHERE proactive_opt_in = true AND last_interaction_timestamp < :thresholdTime")
    Flux<User> findDormantUsersForProactiveMessaging(ZonedDateTime thresholdTime);

    @org.springframework.data.r2dbc.repository.Modifying
    @Query("INSERT INTO users (id, username, timezone) VALUES (:id, 'Anonymous', 'UTC') ON CONFLICT (id) DO NOTHING")
    reactor.core.publisher.Mono<Integer> insertUserIfNotExists(UUID id);
}
