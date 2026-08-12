package com.pm.javagateway.core;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

@Table("users")
public class User {

    @Id
    private UUID id;
    private String username;
    private String timezone;

    public ZonedDateTime getLastInteractionTimestamp() {
        return lastInteractionTimestamp;
    }

    public void setLastInteractionTimestamp(ZonedDateTime lastInteractionTimestamp) {
        this.lastInteractionTimestamp = lastInteractionTimestamp;
    }

    public Boolean getProactiveOptIn() {
        return proactiveOptIn;
    }

    public void setProactiveOptIn(Boolean proactiveOptIn) {
        this.proactiveOptIn = proactiveOptIn;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    private Boolean proactiveOptIn;
    private ZonedDateTime lastInteractionTimestamp;



}
