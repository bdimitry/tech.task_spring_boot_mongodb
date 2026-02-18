package com.example.demo.dto;

import java.util.Map;

public record NoteStatsResponse(
        Map<String, Integer> stats
) {}
