package com.pm.javagateway.core;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Sinks;

@Component
public class SessionRegistry {
    // maps a srting userID to a reactive sink that can emit messages down the
    // websocket
    private final ConcurrentHashMap<String, Sinks.Many<String>> activeSessions = new ConcurrentHashMap<>();

    public void registerSession(String userId, Sinks.Many<String> sink) {
        activeSessions.put(userId, sink);
        System.out.println("SessionRegistory: User " + userId + " connected ");
    }

    public void unregisterSession(String userId) {
        activeSessions.remove(userId);
        System.out.println("SessionRegistory: User " + userId + " disconnected ");
    }

    public boolean isUserConnected(String userId) {
        return activeSessions.containsKey(userId);
    }

    public void pushMessage(String userId, String message) {
        Sinks.Many<String> sink = activeSessions.get(userId);
        if (sink != null) {
            // push the message asyncronously into the user's reactive stream
            sink.tryEmitNext(message);
        }
    }

}
