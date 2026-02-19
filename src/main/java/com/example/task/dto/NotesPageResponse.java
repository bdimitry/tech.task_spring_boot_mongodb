package com.example.task.dto;

import java.util.List;

public record NotesPageResponse(
        List<NoteResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {}
