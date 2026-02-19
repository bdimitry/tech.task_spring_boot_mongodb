package com.example.demo.util;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class WordStatsCalculator {

    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{Nd}]+");

    public Map<String, Integer> countWords(String text) {
        if (text == null || text.isBlank()) {
            return Map.of();
        }

        Map<String, Integer> counts = new HashMap<>();

        for (String raw : NON_WORD.split(text.toLowerCase(Locale.ROOT).trim())) {
            if (raw.isBlank()) continue;
            counts.merge(raw, 1, Integer::sum);
        }

        // сортировка по убыванию count, потом по слову (стабильно)
        return counts.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    return (cmp != 0) ? cmp : a.getKey().compareTo(b.getKey());
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (x, y) -> x,
                        LinkedHashMap::new
                ));
    }
}
