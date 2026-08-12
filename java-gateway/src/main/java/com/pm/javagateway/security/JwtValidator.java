package com.pm.javagateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Base64;

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
}
