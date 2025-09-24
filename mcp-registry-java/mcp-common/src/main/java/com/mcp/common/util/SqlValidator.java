package com.mcp.common.util;

import com.mcp.common.exception.QueryException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL security validation utility
 */
@Component
public class SqlValidator {

    // Default blocked keywords
    private static final Set<String> DEFAULT_BLOCKED_KEYWORDS = Set.of(
        "DROP", "TRUNCATE", "DELETE", "ALTER", "CREATE", "GRANT", "REVOKE"
    );

    // Default allowed operations
    private static final Set<String> DEFAULT_ALLOWED_OPERATIONS = Set.of(
        "SELECT", "INSERT", "UPDATE", "WITH", "EXPLAIN"
    );

    // Dangerous pattern detection
    private static final List<Pattern> DANGEROUS_PATTERNS = Arrays.asList(
        Pattern.compile(".*;.*", Pattern.CASE_INSENSITIVE), // Multiple statements
        Pattern.compile("--.*", Pattern.CASE_INSENSITIVE),  // Comments
        Pattern.compile("/\\*.*\\*/", Pattern.CASE_INSENSITIVE), // Block comments
        Pattern.compile("\\bunion\\s+select\\b", Pattern.CASE_INSENSITIVE), // UNION SELECT
        Pattern.compile("\\bexec\\s*\\(", Pattern.CASE_INSENSITIVE), // Dynamic execution
        Pattern.compile("\\bsp_executesql\\b", Pattern.CASE_INSENSITIVE) // SQL Server dynamic execution
    );

    private final Set<String> blockedKeywords;
    private final Set<String> allowedOperations;
    private final int maxQueryLength;

    public SqlValidator() {
        this.blockedKeywords = DEFAULT_BLOCKED_KEYWORDS;
        this.allowedOperations = DEFAULT_ALLOWED_OPERATIONS;
        this.maxQueryLength = 10000;
    }

    public SqlValidator(Set<String> blockedKeywords,
                       Set<String> allowedOperations,
                       int maxQueryLength) {
        this.blockedKeywords = blockedKeywords != null ? blockedKeywords : DEFAULT_BLOCKED_KEYWORDS;
        this.allowedOperations = allowedOperations != null ? allowedOperations : DEFAULT_ALLOWED_OPERATIONS;
        this.maxQueryLength = maxQueryLength;
    }

    /**
     * Validate SQL query security
     */
    public void validateQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new QueryException("Query cannot be empty");
        }

        String normalizedQuery = query.trim().toUpperCase();

        // Check query length
        if (query.length() > maxQueryLength) {
            throw new QueryException("Query length exceeds maximum allowed: " + maxQueryLength);
        }

        // Check blocked keywords
        validateBlockedKeywords(normalizedQuery);

        // Check allowed operations
        validateAllowedOperations(normalizedQuery);

        // Check dangerous patterns
        validateDangerousPatterns(query);
    }

    /**
     * Check blocked keywords
     */
    private void validateBlockedKeywords(String query) {
        for (String keyword : blockedKeywords) {
            if (containsKeyword(query, keyword)) {
                throw new QueryException.OperationNotAllowed(keyword);
            }
        }
    }

    /**
     * Check allowed operations
     */
    private void validateAllowedOperations(String query) {
        String firstWord = getFirstWord(query);
        if (firstWord != null && !allowedOperations.contains(firstWord)) {
            throw new QueryException.OperationNotAllowed(firstWord);
        }
    }

    /**
     * Check dangerous patterns
     */
    private void validateDangerousPatterns(String query) {
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(query).find()) {
                throw new QueryException.SqlInjectionDetected(
                    "Dangerous pattern detected: " + pattern.pattern()
                );
            }
        }
    }

    /**
     * Check if contains keyword
     */
    private boolean containsKeyword(String query, String keyword) {
        return Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE)
                .matcher(query)
                .find();
    }

    /**
     * Get first word
     */
    private String getFirstWord(String query) {
        String[] words = query.trim().split("\\s+");
        return words.length > 0 ? words[0] : null;
    }

    /**
     * Sanitize query string (remove extra whitespace)
     */
    public String sanitizeQuery(String query) {
        if (query == null) {
            return null;
        }
        return query.trim().replaceAll("\\s+", " ");
    }

    /**
     * Check if it is a read-only query
     */
    public boolean isReadOnlyQuery(String query) {
        if (query == null) {
            return false;
        }

        String normalizedQuery = query.trim().toUpperCase();
        String firstWord = getFirstWord(normalizedQuery);

        return "SELECT".equals(firstWord) ||
               "WITH".equals(firstWord) ||
               "EXPLAIN".equals(firstWord);
    }
}