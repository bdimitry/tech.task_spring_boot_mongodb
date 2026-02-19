package com.example.task.dto;

import com.example.task.model.Tag;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

/**
 * Request payload for creating or updating a note.
 * <p>
 * The owner (userId) is not part of this DTO because it is provided via the URL path
 * (/api/v1/users/{userId}/notes).
 */
public record NoteRequest(
        @NotBlank String title,
        @NotBlank String text,
        Set<Tag> tags
) {}
