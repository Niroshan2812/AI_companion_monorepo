package com.pm.javagateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtValidator {

    // inject secret key from application.properties
    @Value("${jwt.secret}")
    private String jwtSecret;

    /*
     * Converts the raw string secret into a cryptographic SecretKey object.
     * Uses the modern JJWT 0.12.x syntax for key generation.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /*
     * Parses the token and returns true if the signature is valid and the token is not expired.
     */

    public boolean isValid(String token){
        try{
            // syntax check the cryptographic signature
            Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        }catch (  MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e){
            System.err.println("Invalid JWT token: "+e.getMessage());
            return false;
        }
    }


    /**
     * Extracts the Subject (which represents the User ID) from a validated token payload.
     */

    public String extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token).getBody();
        return claims.getSubject();
    }

    // for testing the script
    @PostConstruct
    public void generateDevToken(){
        long expirationTime = System.currentTimeMillis() + (1000 * 60 * 60 * 24);

        String devToken = Jwts.builder()
                .subject("test-user-uuid-1234") // The mock user ID injected into the RAG context
                .issuedAt(new Date())
                .expiration(new Date(expirationTime))
                .signWith(getSignInKey()) // Sign cryptographically using the application.properties secret
                .compact();


        //  Print the token to the console so you can copy it for wscat.
        System.out.println("\n========================================");
        System.out.println("SECURE DEV TOKEN FOR WSCAT:");
        System.out.println(devToken);
        System.out.println("========================================\n");
    }
}
