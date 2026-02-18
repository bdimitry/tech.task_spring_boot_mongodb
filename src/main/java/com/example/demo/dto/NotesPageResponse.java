package com.example.demo.dto;

import java.util.List;

public record NotesPageResponse(
        List<NoteResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {}
