package com.mcp.common.util;

import com.mcp.common.exception.QueryException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL 安全驗證工具
 */
@Component
public class SqlValidator {

    // 預設被阻擋的關鍵字
    private static final Set<String> DEFAULT_BLOCKED_KEYWORDS = Set.of(
        "DROP", "TRUNCATE", "DELETE", "ALTER", "CREATE", "GRANT", "REVOKE"
    );

    // 預設允許的操作
    private static final Set<String> DEFAULT_ALLOWED_OPERATIONS = Set.of(
        "SELECT", "INSERT", "UPDATE", "WITH", "EXPLAIN"
    );

    // 危險模式檢測
    private static final List<Pattern> DANGEROUS_PATTERNS = Arrays.asList(
        Pattern.compile(".*;.*", Pattern.CASE_INSENSITIVE), // 多語句
        Pattern.compile("--.*", Pattern.CASE_INSENSITIVE),  // 註解
        Pattern.compile("/\\*.*\\*/", Pattern.CASE_INSENSITIVE), // 區塊註解
        Pattern.compile("\\bunion\\s+select\\b", Pattern.CASE_INSENSITIVE), // UNION SELECT
        Pattern.compile("\\bexec\\s*\\(", Pattern.CASE_INSENSITIVE), // 動態執行
        Pattern.compile("\\bsp_executesql\\b", Pattern.CASE_INSENSITIVE) // SQL Server 動態執行
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
     * 驗證 SQL 查詢安全性
     */
    public void validateQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new QueryException("Query cannot be empty");
        }

        String normalizedQuery = query.trim().toUpperCase();

        // 檢查查詢長度
        if (query.length() > maxQueryLength) {
            throw new QueryException("Query length exceeds maximum allowed: " + maxQueryLength);
        }

        // 檢查被阻擋的關鍵字
        validateBlockedKeywords(normalizedQuery);

        // 檢查允許的操作
        validateAllowedOperations(normalizedQuery);

        // 檢查危險模式
        validateDangerousPatterns(query);
    }

    /**
     * 檢查被阻擋的關鍵字
     */
    private void validateBlockedKeywords(String query) {
        for (String keyword : blockedKeywords) {
            if (containsKeyword(query, keyword)) {
                throw new QueryException.OperationNotAllowed(keyword);
            }
        }
    }

    /**
     * 檢查允許的操作
     */
    private void validateAllowedOperations(String query) {
        String firstWord = getFirstWord(query);
        if (firstWord != null && !allowedOperations.contains(firstWord)) {
            throw new QueryException.OperationNotAllowed(firstWord);
        }
    }

    /**
     * 檢查危險模式
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
     * 檢查是否包含關鍵字
     */
    private boolean containsKeyword(String query, String keyword) {
        return Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE)
                .matcher(query)
                .find();
    }

    /**
     * 取得第一個單字
     */
    private String getFirstWord(String query) {
        String[] words = query.trim().split("\\s+");
        return words.length > 0 ? words[0] : null;
    }

    /**
     * 清理查詢字串（移除多餘空白）
     */
    public String sanitizeQuery(String query) {
        if (query == null) {
            return null;
        }
        return query.trim().replaceAll("\\s+", " ");
    }

    /**
     * 檢查是否為只讀查詢
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