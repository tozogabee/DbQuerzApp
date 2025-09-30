package examp.org.com.dbquerzapp.validator;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;

@Component
public class SqlValidator {

    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("--.*");
    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static final Pattern[] INJECTION_PATTERNS = {
        Pattern.compile("';.*--", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bunion\\s+select", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bor\\s+1\\s*=\\s*1", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\band\\s+1\\s*=\\s*1", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bor\\s+'.*'\\s*=\\s*'.*'", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\band\\s+'.*'\\s*=\\s*'.*'", Pattern.CASE_INSENSITIVE)
    };

    public ValidationResult validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return ValidationResult.invalid("SQL is null or empty");
        }

        String cleanedSql = cleanSql(sql);

        ValidationResult syntaxResult = validateSqlSyntax(cleanedSql);
        if (!syntaxResult.isValid()) {
            return syntaxResult;
        }

        if (!isValidSqlFormat(cleanedSql)) {
            return ValidationResult.invalid("Invalid SQL format");
        }

        String upperSql = cleanedSql.toUpperCase();
        for (DANGEROUS_KEYWORD keyword : DANGEROUS_KEYWORD.values()) {
            if (containsKeyword(upperSql, keyword.name())) {
                return ValidationResult.invalid("Dangerous SQL keyword detected: " + keyword);
            }
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(cleanedSql).find()) {
                return ValidationResult.invalid("Potential SQL injection detected");
            }
        }

        if (!upperSql.trim().startsWith("SELECT")) {
            return ValidationResult.invalid("Only SELECT statements are allowed");
        }

        if (containsMultipleStatements(cleanedSql)) {
            return ValidationResult.invalid("Multiple SQL statements are not allowed");
        }

        return ValidationResult.valid();
    }

    private String cleanSql(String sql) {
        String cleaned = SINGLE_LINE_COMMENT.matcher(sql).replaceAll("");
        cleaned = MULTI_LINE_COMMENT.matcher(cleaned).replaceAll("");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }

    private boolean isValidSqlFormat(String sql) {
        if (sql.length() < 6) {
            return false;
        }

        int openParens = 0;
        for (char c : sql.toCharArray()) {
            if (c == '(') openParens++;
            else if (c == ')') openParens--;
            if (openParens < 0) return false;
        }
        return openParens == 0;
    }

    private boolean containsKeyword(String sql, String keyword) {
        Pattern pattern = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(sql).find();
    }

    private boolean containsMultipleStatements(String sql) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == ';' && !inSingleQuote && !inDoubleQuote) {
                String remaining = sql.substring(i + 1).trim();
                if (!remaining.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private ValidationResult validateSqlSyntax(String sql) {
        String upperSql = sql.toUpperCase().trim();

        if (!upperSql.startsWith("SELECT")) {
            return ValidationResult.invalid("SQL must start with SELECT");
        }

        if (upperSql.contains("SELECT FORM")) {
            return ValidationResult.invalid("Invalid syntax: 'SELECT FORM' should be 'SELECT FROM'");
        }

        if (upperSql.contains("FORM ")) {
            return ValidationResult.invalid("Invalid syntax: 'FORM' should be 'FROM'");
        }

        if (upperSql.contains(" FROM ")) {
            String[] parts = upperSql.split("\\s+FROM\\s+", 2);
            if (parts.length == 2) {
                String afterFrom = parts[1].trim();
                if (afterFrom.isEmpty()) {
                    return ValidationResult.invalid("Missing table name after FROM");
                }

                // Check if FROM is followed by a valid identifier
                String[] fromParts = afterFrom.split("\\s+", 2);
                String tableName = fromParts[0];
                if (!isValidIdentifier(tableName)) {
                    return ValidationResult.invalid("Invalid table name: " + tableName);
                }
            }
        } else if (upperSql.contains("FROM")) {
            // FROM exists but not properly spaced
            return ValidationResult.invalid("Invalid syntax: FROM clause needs proper spacing");
        }

        // Check for proper WHERE clause structure
        if (upperSql.contains(" WHERE ")) {
            String[] parts = upperSql.split("\\s+WHERE\\s+", 2);
            if (parts.length == 2) {
                String whereClause = parts[1].trim();
                if (whereClause.isEmpty()) {
                    return ValidationResult.invalid("Empty WHERE clause");
                }
            }
        }

        // Check for common misspellings
        if (upperSql.contains("SELCT ") || upperSql.contains("SLECT ")) {
            return ValidationResult.invalid("Invalid syntax: 'SELECT' is misspelled");
        }

        if (upperSql.contains(" WHRE ") || upperSql.contains(" WERE ")) {
            return ValidationResult.invalid("Invalid syntax: 'WHERE' is misspelled");
        }

        if (upperSql.contains(" ODER ") || upperSql.contains(" ORDR ")) {
            return ValidationResult.invalid("Invalid syntax: 'ORDER' is misspelled");
        }

        // Check for unmatched quotes
        if (!hasMatchedQuotes(sql)) {
            return ValidationResult.invalid("Unmatched quotes in SQL");
        }

        return ValidationResult.valid();
    }

    private boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }

        // Remove backticks, quotes if present
        String cleaned = identifier.replaceAll("[`\"'\\[\\]]", "");

        // Check if it starts with letter or underscore and contains only valid characters
        return cleaned.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    private boolean hasMatchedQuotes(String sql) {
        int singleQuotes = 0;
        int doubleQuotes = 0;
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (c == '\'' && !inDouble) {
                if (!inSingle) {
                    singleQuotes++;
                    inSingle = true;
                } else {
                    inSingle = false;
                }
            } else if (c == '"' && !inSingle) {
                if (!inDouble) {
                    doubleQuotes++;
                    inDouble = true;
                } else {
                    inDouble = false;
                }
            }
        }

        return singleQuotes % 2 == 0 && doubleQuotes % 2 == 0;
    }

}