package com.example.task.dto;

import java.util.Map;

public record NoteStatsResponse(
        Map<String, Integer> stats
) {}
