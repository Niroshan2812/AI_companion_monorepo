package com.pm.javagateway.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;


@Component
public class JwtWebSocketHandshakerFilter implements WebFilter {

    private final JwtValidator jwtValidator;

    public JwtWebSocketHandshakerFilter(JwtValidator jwtValidator ) {
        this.jwtValidator = jwtValidator;
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        // check if the incomming req, is targeting the websocket endpoint
        String path = exchange.getRequest().getURI().getPath();
        if(!path.startsWith("/ws/companion")){
            // if it is a standed REST call, Skip this specific webSocket filter
            return chain.filter(exchange);
        }

        //Extract JWT from the quary parm, insted of the auth header
        String token = exchange.getRequest().getQueryParams().getFirst("token");

        // validate tthe presence and cryprographic signature of the JWT
        if(token == null || !jwtValidator.isValid(token)){
            // if invalid terminate the handsheke with 401
            // prevent tcp socket
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Extract the userID from the validated JWT
        String userId = jwtValidator.extractUserId(token);

        // Matate the exchange to inject the validated userID into the request attributres
        // so thire is an secure identity accessible to the downstream webSocketHandler
        exchange.getAttributes().put("SECURE_USER_ID", userId);

        //yeid contol back to the reactive chain to complete the http to wss upgrade
        return chain.filter(exchange);
    }
}
