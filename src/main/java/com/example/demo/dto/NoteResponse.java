package com.example.demo.dto;

import com.example.demo.model.Tag;

import java.time.Instant;
import java.util.Set;

public record NoteResponse(
        String id,
        String title,
        String userId,
        Instant createdDate,
        String text,
        Set<Tag> tags
) {}
