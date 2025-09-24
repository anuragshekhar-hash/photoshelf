package org.photoshelf;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class KeywordSuggestion {

    private final KeywordManager keywordManager;

    public KeywordSuggestion(KeywordManager keywordManager) {
        this.keywordManager = keywordManager;
    }

    public Set<String> getSuggestions(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return new TreeSet<>(); // Return empty set for empty prefix
        }
        String lowerCasePrefix = prefix.toLowerCase();
        return keywordManager.getAllKeywords().stream()
                .filter(keyword -> keyword.toLowerCase().startsWith(lowerCasePrefix))
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
