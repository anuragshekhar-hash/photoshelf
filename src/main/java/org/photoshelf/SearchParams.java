package org.photoshelf;

import java.util.List;
import java.util.Set;

public class SearchParams {
    private boolean recursive;
    private String keyword;
    private String filter;
    private String searchString;
    private boolean hasKeyword;
    private boolean hasFilter;
    private boolean hasSearchString;
    boolean noKeyword = false;
    List<String> keywords;
    private String expression;
    private KeywordExpressionEvaluator evaluater;
    private Set<String> allowedExtensions;

    public List<String> getKeywords() {
        return keywords;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        hasKeyword = true;
        this.keyword = keyword;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        hasFilter = true;
        this.filter = filter;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        hasSearchString = true;
        this.searchString = searchString;
    }

    public boolean hasKeyword() {
        return hasKeyword;
    }

    public boolean hasFilter() {
        return hasFilter;
    }

    public boolean hasSearchString() {
        return hasSearchString;
    }

    public boolean isNoKeywords() {
        return noKeyword;
    }

    public void setNoKeywords(boolean noKeyword) {
        hasKeyword = true;
        this.noKeyword = noKeyword;
    }

    public void setKeywords(List<String> keywords) {
        if (!keywords.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String keyword:keywords) {
              if (!sb.isEmpty()) {
                  sb.append('|');
              }
              sb.append(keyword);
            }
            setExpression(sb.toString());
        }
    }

    public void setExpression(String expression) {
        if (!expression.isBlank())
            hasKeyword = true;
        this.expression = expression;
    }

    public boolean evaluate(Set<String> keywords) {
        if (evaluater == null) {
            evaluater = new KeywordExpressionEvaluator(expression);
        }
        return evaluater.evaluate(keywords);
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(Set<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }
}
