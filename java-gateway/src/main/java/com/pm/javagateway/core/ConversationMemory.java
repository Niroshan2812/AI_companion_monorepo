package com.pm.javagateway.core;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

@Table("conversation_memory")
public class ConversationMemory {
    @Id
    private UUID id;
    private UUID userId;
    private String prompt;
    private String response;
    // Step 2: Omit the raw vector embedding field here.
    // We handle it explicitly via custom queries to avoid R2DBC deserialization crashes.
    private ZonedDateTime timestamp;

    // Standard Getters and Setters
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
}
