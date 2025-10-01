package examp.org.com.dbquerzapp.service;

import examp.org.com.dbquerzapp.validator.SqlValidator;
import examp.org.com.dbquerzapp.validator.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SqlValidator sqlValidator;

    @Mock
    private PathMatchingResourcePatternResolver resolver;

    private QueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new QueryService();
        ReflectionTestUtils.setField(queryService, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(queryService, "sqlValidator", sqlValidator);
        ReflectionTestUtils.setField(queryService, "resolver", resolver);
    }

    @Test
    @DisplayName("Should load query from existing file successfully")
    void testLoadQueryFromFileSuccess() throws IOException {
        // Given
        String queryIdentifier = "test_query";
        String expectedSql = "SELECT * FROM users;";
        Resource mockResource = new ByteArrayResource(expectedSql.getBytes(StandardCharsets.UTF_8));

        when(resolver.getResource("classpath:queries/test_query.sql")).thenReturn(mockResource);

        // When
        String result = queryService.loadQueryFromFile(queryIdentifier);

        // Then
        assertEquals(expectedSql, result);
        verify(resolver).getResource("classpath:queries/test_query.sql");
    }

    @Test
    @DisplayName("Should throw IOException when file does not exist")
    void testLoadQueryFromFileNotFound() throws IOException {
        // Given
        String queryIdentifier = "nonexistent_query";
        Resource mockResource = mock(Resource.class);

        when(resolver.getResource("classpath:queries/nonexistent_query.sql")).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(false);

        // When & Then
        IOException exception = assertThrows(IOException.class, () -> {
            queryService.loadQueryFromFile(queryIdentifier);
        });

        assertTrue(exception.getMessage().contains("Query file not found"));
        verify(resolver).getResource("classpath:queries/nonexistent_query.sql");
        verify(mockResource).exists();
    }

    @Test
    @DisplayName("Should throw IOException when file reading fails")
    void testLoadQueryFromFileReadError() throws IOException {
        // Given
        String queryIdentifier = "test_query";
        Resource mockResource = mock(Resource.class);

        when(resolver.getResource("classpath:queries/test_query.sql")).thenReturn(mockResource);
        when(mockResource.exists()).thenReturn(false);

        // When & Then
        IOException exception = assertThrows(IOException.class, () -> {
            queryService.loadQueryFromFile(queryIdentifier);
        });

        assertTrue(exception.getMessage().contains("Query file not found"));
        verify(resolver).getResource("classpath:queries/test_query.sql");
    }

    @Test
    @DisplayName("Should validate SQL using SqlValidator")
    void testValidateSql() {
        // Given
        String sql = "SELECT * FROM users";
        ValidationResult expectedResult = ValidationResult.valid();
        when(sqlValidator.validateSql(sql)).thenReturn(expectedResult);

        // When
        ValidationResult result = queryService.validateSql(sql);

        // Then
        assertEquals(expectedResult, result);
        verify(sqlValidator).validateSql(sql);
    }

    @Test
    @DisplayName("Should execute valid SQL successfully")
    void testExecuteQuerySuccess() {
        // Given
        String sql = "SELECT * FROM users";
        ValidationResult validResult = ValidationResult.valid();
        List<Map<String, Object>> expectedResults = Arrays.asList(
            Map.of("id", 1, "name", "John"),
            Map.of("id", 2, "name", "Jane")
        );

        when(sqlValidator.validateSql(sql)).thenReturn(validResult);
        when(jdbcTemplate.queryForList(sql)).thenReturn(expectedResults);

        // When
        List<Map<String, Object>> results = queryService.executeQuery(sql);

        // Then
        assertEquals(expectedResults, results);
        verify(sqlValidator).validateSql(sql);
        verify(jdbcTemplate).queryForList(sql);
    }

    @Test
    @DisplayName("Should throw exception for invalid SQL")
    void testExecuteQueryInvalidSql() {
        // Given
        String sql = "SELECT FORM users";
        ValidationResult invalidResult = ValidationResult.invalid("Invalid SQL syntax");

        when(sqlValidator.validateSql(sql)).thenReturn(invalidResult);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            queryService.executeQuery(sql);
        });

        assertTrue(exception.getMessage().contains("SQL validation failed"));
        assertTrue(exception.getMessage().contains("Invalid SQL syntax"));
        verify(sqlValidator).validateSql(sql);
        verify(jdbcTemplate, never()).queryForList(any());
    }

    @Test
    @DisplayName("Should handle SQL execution errors")
    void testExecuteQuerySqlExecutionError() {
        // Given
        String sql = "SELECT * FROM nonexistent_table";
        ValidationResult validResult = ValidationResult.valid();

        when(sqlValidator.validateSql(sql)).thenReturn(validResult);
        when(jdbcTemplate.queryForList(sql)).thenThrow(new RuntimeException("Table does not exist"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            queryService.executeQuery(sql);
        });

        assertEquals("Table does not exist", exception.getMessage());
        verify(sqlValidator).validateSql(sql);
        verify(jdbcTemplate).queryForList(sql);
    }

    @Test
    @DisplayName("Should handle empty query results")
    void testExecuteQueryEmptyResults() {
        // Given
        String sql = "SELECT * FROM users WHERE id = 999";
        ValidationResult validResult = ValidationResult.valid();
        List<Map<String, Object>> emptyResults = new ArrayList<>();

        when(sqlValidator.validateSql(sql)).thenReturn(validResult);
        when(jdbcTemplate.queryForList(sql)).thenReturn(emptyResults);

        // When
        List<Map<String, Object>> results = queryService.executeQuery(sql);

        // Then
        assertTrue(results.isEmpty());
        verify(sqlValidator).validateSql(sql);
        verify(jdbcTemplate).queryForList(sql);
    }

    @Test
    @DisplayName("Should handle complex query results")
    void testExecuteQueryComplexResults() {
        // Given
        String sql = "SELECT id, name, email, created_date FROM users";
        ValidationResult validResult = ValidationResult.valid();
        List<Map<String, Object>> expectedResults = Arrays.asList(
            Map.of("id", 1, "name", "John", "email", "john@example.com", "created_date", new Date()),
            Map.of("id", 2, "name", "Jane", "email", "jane@example.com", "created_date", new Date())
        );

        when(sqlValidator.validateSql(sql)).thenReturn(validResult);
        when(jdbcTemplate.queryForList(sql)).thenReturn(expectedResults);

        // When
        List<Map<String, Object>> results = queryService.executeQuery(sql);

        // Then
        assertEquals(expectedResults, results);
        assertEquals(2, results.size());
        assertEquals("John", results.get(0).get("name"));
        assertEquals("jane@example.com", results.get(1).get("email"));
        verify(sqlValidator).validateSql(sql);
        verify(jdbcTemplate).queryForList(sql);
    }

    @Test
    @DisplayName("Should handle null SQL validation")
    void testValidateSqlNull() {
        // Given
        String sql = null;
        ValidationResult expectedResult = ValidationResult.invalid("SQL is null or empty");
        when(sqlValidator.validateSql(sql)).thenReturn(expectedResult);

        // When
        ValidationResult result = queryService.validateSql(sql);

        // Then
        assertFalse(result.isValid());
        assertEquals("SQL is null or empty", result.getErrorMessage());
        verify(sqlValidator).validateSql(sql);
    }

    @Test
    @DisplayName("Should handle SQL with dangerous keywords")
    void testExecuteQueryDangerousKeywords() {
        // Given
        String sql = "DROP TABLE users";
        ValidationResult invalidResult = ValidationResult.invalid("Dangerous SQL keyword detected: DROP");

        when(sqlValidator.validateSql(sql)).thenReturn(invalidResult);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            queryService.executeQuery(sql);
        });

        assertTrue(exception.getMessage().contains("SQL validation failed"));
        assertTrue(exception.getMessage().contains("Dangerous SQL keyword detected"));
        verify(sqlValidator).validateSql(sql);
        verify(jdbcTemplate, never()).queryForList(any());
    }
}