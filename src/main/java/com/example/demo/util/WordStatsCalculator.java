package com.example.demo.util;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility component to compute word frequency statistics.
 * <p>
 * It normalizes text to lower-case, splits by non-letter/non-digit characters,
 * counts occurrences and returns a map sorted by count (desc) and then by word.
 */
@Component
public class WordStatsCalculator {

    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{Nd}]+");

    /**
     * Counts words in the given text and returns a sorted frequency map.
     *
     * @param text input note text
     * @return map: word -> occurrences, sorted by occurrences desc
     */
    public Map<String, Integer> countWords(String text) {
        if (text == null || text.isBlank()) {
            return Map.of();
        }

        Map<String, Integer> counts = new HashMap<>();

        for (String raw : NON_WORD.split(text.toLowerCase(Locale.ROOT).trim())) {
            if (raw.isBlank()) continue;
            counts.merge(raw, 1, Integer::sum);
        }

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
