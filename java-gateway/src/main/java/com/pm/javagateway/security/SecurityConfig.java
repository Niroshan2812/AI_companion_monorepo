package com.pm.javagateway.security;
/*
Create this file to tell Spring Security to step back and
let our custom JWT filter handle the authentication for the WebSocket endpoint.
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {
   @Bean
   public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http){
       // disable default spring security features
       return http
               .csrf(ServerHttpSecurity.CsrfSpec::disable)
               .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
               .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
               .authorizeExchange(exchangers -> exchangers
                       .pathMatchers("/ws/companion").permitAll()
                       .anyExchange().permitAll()
               )
               .build();
   }
}
