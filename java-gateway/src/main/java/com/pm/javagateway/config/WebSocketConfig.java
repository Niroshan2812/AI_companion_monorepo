package com.pm.javagateway.config;

import com.pm.javagateway.security.CompanionWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketConfig {

    private final CompanionWebSocketHandler webSocketHandler;

    // Inject our custom handler
    public WebSocketConfig(CompanionWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Bean
    public HandlerMapping webSocketMapping() {
        // Map the specific URL path to our WebSocket handler logic.
        Map<String, Object> map = new HashMap<>();
        map.put("/ws/companion", webSocketHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);

        //  Set the order to -1 so this mapping takes priority over standard REST controllers.
        mapping.setOrder(-1);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        // Required adapter to actually execute the WebSocket upgrade handshake in WebFlux.
        return new WebSocketHandlerAdapter();
    }
}