package com.example.demo.util;

import java.util.*;
import java.util.stream.Collectors;

public class WordStatsCalculator {

    public Map<String, Integer> countWords(String text) {
        if (text == null || text.isBlank()) {
            return Map.of();
        }

        String[] tokens = text.toLowerCase().split("\\P{L}+"); // всё что НЕ буква = разделитель
        Map<String, Integer> freq = new HashMap<>();

        for (String t : tokens) {
            if (t == null || t.isBlank()) continue;
            freq.merge(t, 1, Integer::sum);
        }

        return freq.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    return a.getKey().compareTo(b.getKey());
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (x, y) -> x,
                        LinkedHashMap::new
                ));
    }
}
