package pl.piomin.services.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ProfileResponse {

    private UUID sub;
    private String username;
    private String displayName;
    private String email;
    private boolean hasAvatar;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ProfileResponse() {
    }

    public ProfileResponse(UUID sub, String username, String displayName, String email,
                           boolean hasAvatar, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.sub = sub;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.hasAvatar = hasAvatar;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getSub() {
        return sub;
    }

    public void setSub(UUID sub) {
        this.sub = sub;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isHasAvatar() {
        return hasAvatar;
    }

    public void setHasAvatar(boolean hasAvatar) {
        this.hasAvatar = hasAvatar;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
