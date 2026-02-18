package com.example.demo.dto;

import com.example.demo.model.Tag;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record NoteRequest(
        @NotBlank String title,
        @NotBlank String text,
        Set<Tag> tags
) {}
