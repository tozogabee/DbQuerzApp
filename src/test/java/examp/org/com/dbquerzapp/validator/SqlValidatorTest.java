package examp.org.com.dbquerzapp.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SqlValidatorTest {

    private SqlValidator sqlValidator;

    @BeforeEach
    void setUp() {
        sqlValidator = new SqlValidator();
    }

    @Test
    @DisplayName("Valid SELECT statements should pass validation")
    void testValidSelectStatements() {
        String[] validSqls = {
            "SELECT * FROM users",
            "SELECT id, name FROM users",
            "SELECT u.name FROM users u",
            "SELECT * FROM users WHERE id = 1",
            "SELECT name FROM users WHERE active = true",
            "SELECT COUNT(*) FROM users",
            "SELECT * FROM users ORDER BY name",
            "SELECT DISTINCT name FROM users",
            "SELECT * FROM users LIMIT 10"
        };

        for (String sql : validSqls) {
            ValidationResult result = sqlValidator.validateSql(sql);
            assertTrue(result.isValid(), "SQL should be valid: " + sql);
            assertNull(result.getErrorMessage());
        }
    }

    @Test
    @DisplayName("Null or empty SQL should be invalid")
    void testNullOrEmptySql() {
        ValidationResult result1 = sqlValidator.validateSql(null);
        assertFalse(result1.isValid());
        assertEquals("SQL is null or empty", result1.getErrorMessage());

        ValidationResult result2 = sqlValidator.validateSql("");
        assertFalse(result2.isValid());
        assertEquals("SQL is null or empty", result2.getErrorMessage());

        ValidationResult result3 = sqlValidator.validateSql("   ");
        assertFalse(result3.isValid());
        assertEquals("SQL is null or empty", result3.getErrorMessage());
    }

    @Test
    @DisplayName("Common syntax errors should be caught")
    void testCommonSyntaxErrors() {
        ValidationResult result1 = sqlValidator.validateSql("SELECT FORM users");
        assertFalse(result1.isValid());
        assertTrue(result1.getErrorMessage().contains("'FORM' should be 'FROM'"));

        ValidationResult result2 = sqlValidator.validateSql("SELECT * FORM users");
        assertFalse(result2.isValid());
        assertTrue(result2.getErrorMessage().contains("'SELECT FORM' should be 'SELECT FROM'"));

        ValidationResult result3 = sqlValidator.validateSql("SELCT * FROM users");
        assertFalse(result3.isValid());
        assertTrue(result3.getErrorMessage().contains("'SELECT' is misspelled"));

        ValidationResult result4 = sqlValidator.validateSql("SLECT * FROM users");
        assertFalse(result4.isValid());
        assertTrue(result4.getErrorMessage().contains("'SELECT' is misspelled"));
    }

    @Test
    @DisplayName("Missing table name should be invalid")
    void testMissingTableName() {
        ValidationResult result = sqlValidator.validateSql("SELECT * FROM");
        assertFalse(result.isValid());
        assertEquals("Missing table name after FROM", result.getErrorMessage());
    }

    @Test
    @DisplayName("Invalid table names should be rejected")
    void testInvalidTableNames() {
        String[] invalidTableNames = {
            "SELECT * FROM 123invalid",
            "SELECT * FROM @invalid",
            "SELECT * FROM #invalid",
            "SELECT * FROM $invalid"
        };

        for (String sql : invalidTableNames) {
            ValidationResult result = sqlValidator.validateSql(sql);
            assertFalse(result.isValid(), "Should reject invalid table name: " + sql);
            assertTrue(result.getErrorMessage().contains("Invalid table name"));
        }
    }

    @Test
    @DisplayName("Empty WHERE clause should be invalid")
    void testEmptyWhereClause() {
        ValidationResult result = sqlValidator.validateSql("SELECT * FROM users WHERE");
        assertFalse(result.isValid());
        assertEquals("Empty WHERE clause", result.getErrorMessage());
    }

    @Test
    @DisplayName("Common misspellings should be caught")
    void testCommonMisspellings() {
        ValidationResult result1 = sqlValidator.validateSql("SELECT * FROM users WHRE id = 1");
        assertFalse(result1.isValid());
        assertTrue(result1.getErrorMessage().contains("'WHERE' is misspelled"));

        ValidationResult result2 = sqlValidator.validateSql("SELECT * FROM users WERE id = 1");
        assertFalse(result2.isValid());
        assertTrue(result2.getErrorMessage().contains("'WHERE' is misspelled"));

        ValidationResult result3 = sqlValidator.validateSql("SELECT * FROM users ORDER BY name ODER");
        assertFalse(result3.isValid());
        assertTrue(result3.getErrorMessage().contains("'ORDER' is misspelled"));
    }

    @Test
    @DisplayName("Unmatched quotes should be invalid")
    void testUnmatchedQuotes() {
        String[] unmatchedQuotesSqls = {
            "SELECT * FROM users WHERE name = 'test",
            "SELECT * FROM users WHERE name = \"test",
            "SELECT * FROM users WHERE name = 'test' AND description = \"incomplete"
        };

        for (String sql : unmatchedQuotesSqls) {
            ValidationResult result = sqlValidator.validateSql(sql);
            assertFalse(result.isValid(), "Should reject unmatched quotes: " + sql);
            assertEquals("Unmatched quotes in SQL", result.getErrorMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "DROP TABLE users",
        "DELETE FROM users",
        "UPDATE users SET name = 'test'",
        "INSERT INTO users VALUES (1, 'test')",
        "ALTER TABLE users ADD COLUMN test VARCHAR(50)",
        "CREATE TABLE test (id INT)",
        "TRUNCATE TABLE users",
        "EXEC sp_test",
        "EXECUTE sp_test"
    })
    @DisplayName("Dangerous keywords should be rejected")
    void testDangerousKeywords(String sql) {
        ValidationResult result = sqlValidator.validateSql(sql);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Dangerous SQL keyword detected"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT * FROM users'; DROP TABLE users; --",
        "SELECT * FROM users UNION SELECT password FROM admin",
        "SELECT * FROM users WHERE 1=1",
        "SELECT * FROM users WHERE 'a'='a'",
        "SELECT * FROM users WHERE id = 1 OR 1=1"
    })
    @DisplayName("SQL injection patterns should be detected")
    void testSqlInjectionPatterns(String sql) {
        ValidationResult result = sqlValidator.validateSql(sql);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Potential SQL injection detected") ||
                  result.getErrorMessage().contains("Dangerous SQL keyword detected"));
    }

    @Test
    @DisplayName("Non-SELECT statements should be rejected")
    void testNonSelectStatements() {
        String[] nonSelectSqls = {
            "UPDATE users SET name = 'test'",
            "INSERT INTO users VALUES (1, 'test')",
            "DELETE FROM users WHERE id = 1"
        };

        for (String sql : nonSelectSqls) {
            ValidationResult result = sqlValidator.validateSql(sql);
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("Only SELECT statements are allowed") ||
                      result.getErrorMessage().contains("Dangerous SQL keyword detected"));
        }
    }

    @Test
    @DisplayName("Multiple statements should be rejected")
    void testMultipleStatements() {
        String[] multipleSqls = {
            "SELECT * FROM users; SELECT * FROM orders;",
            "SELECT * FROM users; DROP TABLE users;",
            "SELECT name FROM users; UPDATE users SET active = 1;"
        };

        for (String sql : multipleSqls) {
            ValidationResult result = sqlValidator.validateSql(sql);
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("Multiple SQL statements are not allowed") ||
                      result.getErrorMessage().contains("Dangerous SQL keyword detected"));
        }
    }

    @Test
    @DisplayName("SQL comments should be handled properly")
    void testSqlComments() {
        String[] sqlsWithComments = {
            "SELECT * FROM users -- this is a comment",
            "SELECT * /* comment */ FROM users",
            "SELECT * FROM users /* multi\nline\ncomment */",
            "-- comment at start\nSELECT * FROM users"
        };

        for (String sql : sqlsWithComments) {
            ValidationResult result = sqlValidator.validateSql(sql);
            assertTrue(result.isValid(), "SQL with comments should be valid: " + sql);
        }
    }

    @Test
    @DisplayName("SQL with proper parentheses should be valid")
    void testBalancedParentheses() {
        String[] validParenthesesSqls = {
            "SELECT * FROM users WHERE (id = 1 OR id = 2)",
            "SELECT COUNT(*) FROM users",
            "SELECT * FROM users WHERE id IN (1, 2, 3)",
            "SELECT (CASE WHEN active = 1 THEN 'Yes' ELSE 'No' END) FROM users"
        };

        for (String sql : validParenthesesSqls) {
            ValidationResult result = sqlValidator.validateSql(sql);
            assertTrue(result.isValid(), "SQL with balanced parentheses should be valid: " + sql);
        }
    }

    @Test
    @DisplayName("SQL with unbalanced parentheses should be invalid")
    void testUnbalancedParentheses() {
        String[] invalidParenthesesSqls = {
            "SELECT * FROM users WHERE (id = 1",
            "SELECT * FROM users WHERE id = 1)",
            "SELECT * FROM users WHERE ((id = 1)",
            "SELECT COUNT(*)) FROM users"
        };

        for (String sql : invalidParenthesesSqls) {
            ValidationResult result = sqlValidator.validateSql(sql);
            assertFalse(result.isValid(), "SQL with unbalanced parentheses should be invalid: " + sql);
        }
    }

    @Test
    @DisplayName("ValidationResult static methods should work correctly")
    void testValidationResult() {
        ValidationResult validResult = ValidationResult.valid();
        assertTrue(validResult.isValid());
        assertNull(validResult.getErrorMessage());

        ValidationResult invalidResult = ValidationResult.invalid("Test error");
        assertFalse(invalidResult.isValid());
        assertEquals("Test error", invalidResult.getErrorMessage());
    }
}