package examp.org.com.dbquerzapp.validator;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SqlValidator {

    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("--.*");
    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern INVALID_IDENTIFIER = Pattern.compile("(?<!`|\"|')\\b[0-9@#$%^&*\\-+=\\[\\]{}|;:?!~](?=.*[a-zA-Z_])[a-zA-Z0-9_]+\\b(?!`|\"|')");
    private static final String IDENTIFIER = "(?:`[^`]+`|\"[^\"]+\"|[A-Za-z_][A-Za-z0-9_]*)";
    private static final String QUALIFIED_IDENTIFIER = IDENTIFIER + "(?:\\s*\\.\\s*" + IDENTIFIER + ")?";
    private static final String STRING_LITERAL = "'(?:''|[^'])*'";
    private static final String NUMBER_LITERAL = "\\d+(?:\\.\\d+)?";
    private static final String VALUE = "(?:" + STRING_LITERAL + "|" + NUMBER_LITERAL + "|" + QUALIFIED_IDENTIFIER + ")";
    private static final String FUNCTION_CALL = QUALIFIED_IDENTIFIER + "\\s*\\(\\s*(?:\\*|" + VALUE + "(?:\\s*,\\s*" + VALUE + ")*)?\\s*\\)";
    private static final String CASE_EXPR = "\\(\\s*CASE(?:\\s+WHEN\\s+.+?\\s+THEN\\s+.+?)+(?:\\s+ELSE\\s+.+?)?\\s+END\\s*\\)";
    private static final String COLUMN_EXPR = "(?:" + CASE_EXPR + "|" + FUNCTION_CALL + "|" + QUALIFIED_IDENTIFIER + ")";
    private static final String COLUMN_ALIAS = "(?:\\s+(?:AS\\s+)?" + IDENTIFIER + ")?";
    private static final String SELECT_ITEM = COLUMN_EXPR + COLUMN_ALIAS;
    private static final String SELECT_LIST = "(?:\\*|" + SELECT_ITEM + "(?:\\s*,\\s*" + SELECT_ITEM + ")*)";
    private static final String TABLE_NAME = QUALIFIED_IDENTIFIER + "(?:\\s+(?:AS\\s+)?" + IDENTIFIER + ")?";
    private static final String COMPARISON_OP = "(?:=|!=|<>|<=|>=|<|>|LIKE)";
    private static final String WHERE_OPERAND = "(?:" + FUNCTION_CALL + "|" + VALUE + ")";
    private static final String SIMPLE_CONDITION = WHERE_OPERAND + "\\s*" + COMPARISON_OP + "\\s*" + WHERE_OPERAND + "(?:\\s+ESCAPE\\s+" + STRING_LITERAL + ")?";
    private static final String NULL_CONDITION = WHERE_OPERAND + "\\s+IS\\s+(?:NOT\\s+)?NULL";
    private static final String IN_CONDITION = WHERE_OPERAND + "\\s+(?:NOT\\s+)?IN\\s*\\(\\s*" + VALUE + "(?:\\s*,\\s*" + VALUE + ")*\\s*\\)";
    private static final String BETWEEN_CONDITION = WHERE_OPERAND + "\\s+(?:NOT\\s+)?BETWEEN\\s+" + VALUE + "\\s+AND\\s+" + VALUE;
    private static final String BASIC_CONDITION = "(?:" + SIMPLE_CONDITION + "|" + NULL_CONDITION + "|" + IN_CONDITION + "|" + BETWEEN_CONDITION + ")";
    private static final String WHERE_CLAUSE = "(?:\\(\\s*)*" + BASIC_CONDITION + "(?:\\s*\\))*(?:\\s+(?:AND|OR)\\s+(?:\\(\\s*)*" + BASIC_CONDITION + "(?:\\s*\\))*)*";
    private static final String GROUP_BY_ITEM = COLUMN_EXPR;
    private static final String GROUP_BY = "GROUP\\s+BY\\s+" + GROUP_BY_ITEM + "(?:\\s*,\\s*" + GROUP_BY_ITEM + ")*";
    private static final String HAVING = "HAVING\\s+" + WHERE_CLAUSE;
    private static final String ORDER_DIRECTION = "(?:ASC|DESC)";
    private static final String ORDER_BY_ITEM = COLUMN_EXPR + "(?:\\s+" + ORDER_DIRECTION + ")?";
    private static final String ORDER_BY = "ORDER\\s+BY\\s+" + ORDER_BY_ITEM + "(?:\\s*,\\s*" + ORDER_BY_ITEM + ")*";
    private static final String LIMIT = "LIMIT\\s+\\d+(?:\\s+OFFSET\\s+\\d+)?";
    // UNION clause
    private static final String UNION_CLAUSE = "UNION(?:\\s+ALL)?\\s+SELECT\\s+" + SELECT_LIST + "\\s+FROM\\s+" + TABLE_NAME + "(?:\\s+WHERE\\s+" + WHERE_CLAUSE + ")?";

    private static final Pattern SELECT_SQL_PATTERN = Pattern.compile(
        "^(?i)(?!.*\\s+WHERE\\s*$)" +
        "SELECT\\s+" + SELECT_LIST +
        "\\s+FROM\\s+" + TABLE_NAME +
        "(?:\\s+WHERE\\s+" + WHERE_CLAUSE + ")?" +
        "(?:\\s+" + GROUP_BY + ")?" +
        "(?:\\s+" + HAVING + ")?" +
        "(?:\\s+" + ORDER_BY + ")?" +
        "(?:\\s+" + LIMIT + ")?" +
        "(?:\\s+" + UNION_CLAUSE + ")*$"  // Allow multiple UNION clauses
    );

    private static final Pattern[] INJECTION_PATTERNS = {
        Pattern.compile("';.*--", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:WHERE|OR|AND)\\s+1\\s*=\\s*1", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:WHERE|OR|AND)\\s+0\\s*=\\s*0", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:WHERE|OR|AND)\\s+'[^']*'\\s*=\\s*'[^']*'", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bor\\s+'.*'\\s*=\\s*'.*'", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\band\\s+'.*'\\s*=\\s*'.*'", Pattern.CASE_INSENSITIVE)
    };

    public ValidationResult validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return ValidationResult.invalid("SQL is null or empty");
        }

        String cleanedSql = cleanSql(sql);
        String upperSql = cleanedSql.toUpperCase();

        for (DANGEROUS_KEYWORD keyword : DANGEROUS_KEYWORD.values()) {
            if (containsKeyword(upperSql, keyword.name())) {
                return ValidationResult.invalid("Dangerous SQL keyword detected: " + keyword);
            }
        }

        ValidationResult syntaxResult = validateSelectSqlSyntax(cleanedSql);
        if (!syntaxResult.isValid()) {
            return syntaxResult;
        }

        if (!isValidSqlFormat(cleanedSql)) {
            return ValidationResult.invalid("Invalid SQL format");
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

    private ValidationResult validateSelectSqlSyntax(String sql) {
        String trimmedSql = sql.trim();

        // Check for invalid identifiers (table/column names starting with numbers)
        if (INVALID_IDENTIFIER.matcher(sql).find()) {
            return ValidationResult.invalid("Invalid identifier: table or column names cannot start with numbers");
        }

        if (!SELECT_SQL_PATTERN.matcher(trimmedSql).matches()) {
            return ValidationResult.invalid("Invalid SELECT SQL syntax. Expected format: SELECT columns FROM table [WHERE conditions] [GROUP BY ...] [HAVING ...] [ORDER BY ...] [LIMIT ...]");
        }

        return ValidationResult.valid();
    }

}