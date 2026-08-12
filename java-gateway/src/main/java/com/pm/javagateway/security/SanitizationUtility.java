package com.pm.javagateway.security;


import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SanitizationUtility {

    // Pattern to match basic HTML/XML tags
    private static final Pattern HTML_TAGS_PATTERN = Pattern.compile("<[^>]*>");

    // Pattern to match common system prompt injection keyword
    private static final Pattern SYSTEM_INSTRUCTION_PATTERN = Pattern.compile(
            "(?i)(\\[system:|\\[instruction:|ignore previous|you are now|system prompt)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Cleans the raw user input by stripping HTML tags and known injection vectors.
     *
     * @param rawInput The un-trusted string received from the WebSocket client.
     * @return A sanitized string safe to pass to the RAG database and Python Engine.
     */

    public String stripInjectionVectors(String rawInput){
        if(rawInput==null || rawInput.trim().isEmpty()){
            return "";
        }

        // remove all HTML tags to prevent client-side script injection
        String sanitized = HTML_TAGS_PATTERN.matcher(rawInput).replaceAll("");

        // strip out explicit System override attempts
        sanitized = SYSTEM_INSTRUCTION_PATTERN.matcher(rawInput).replaceAll("");

        sanitized = sanitized.replace("{", "")
                .replace("}", "")
                .replace("\\", "");

        return sanitized.trim();
    }
}
