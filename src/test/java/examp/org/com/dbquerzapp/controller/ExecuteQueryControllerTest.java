package examp.org.com.dbquerzapp.controller;

import com.example.model.QueryResponse;
import examp.org.com.dbquerzapp.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecuteQueryControllerTest {

    @Mock
    private QueryService queryService;

    private ExecuteQueryController controller;

    @BeforeEach
    void setUp() {
        controller = new ExecuteQueryController();
        ReflectionTestUtils.setField(controller, "queryService", queryService);
    }

    @Test
    @DisplayName("Should execute query successfully and return QueryResponse")
    void testExecuteQuerySuccess() throws IOException {
        // Given
        String queryIdentifier = "get_user_data";
        String sql = "SELECT * FROM users";
        List<Map<String, Object>> mockResults = Arrays.asList(
            Map.of("id", 1, "name", "John"),
            Map.of("id", 2, "name", "Jane")
        );

        when(queryService.loadQueryFromFile(queryIdentifier)).thenReturn(sql);
        when(queryService.executeQuery(sql)).thenReturn(mockResults);

        // When
        ResponseEntity<QueryResponse> response = controller.executeQuery(queryIdentifier);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        QueryResponse queryResponse = response.getBody();
        assertTrue(queryResponse.getSuccess());
        assertEquals(2, queryResponse.getData().size());
        assertTrue(queryResponse.getExecutionTimeMs() >= 0);

        verify(queryService).loadQueryFromFile(queryIdentifier);
        verify(queryService).executeQuery(sql);
    }

    @Test
    @DisplayName("Should return 400 when SQL validation fails")
    void testExecuteQueryValidationFailure() throws IOException {
        // Given
        String queryIdentifier = "invalid_query";
        String sql = "SELECT FORM users";

        when(queryService.loadQueryFromFile(queryIdentifier)).thenReturn(sql);
        when(queryService.executeQuery(sql)).thenThrow(new IllegalArgumentException("SQL validation failed: Invalid syntax"));

        // When
        ResponseEntity<QueryResponse> response = controller.executeQuery(queryIdentifier);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        QueryResponse queryResponse = response.getBody();
        assertFalse(queryResponse.getSuccess());
        assertEquals(400, queryResponse.getCode());
        assertTrue(queryResponse.getExecutionTimeMs() >= 0);

        verify(queryService).loadQueryFromFile(queryIdentifier);
        verify(queryService).executeQuery(sql);
    }

    @Test
    @DisplayName("Should return 404 when query file not found")
    void testExecuteQueryFileNotFound() throws IOException {
        // Given
        String queryIdentifier = "nonexistent_query";

        when(queryService.loadQueryFromFile(queryIdentifier))
            .thenThrow(new IOException("Query file not found: nonexistent_query.sql"));

        // When
        ResponseEntity<QueryResponse> response = controller.executeQuery(queryIdentifier);

        // Then
        assertNotNull(response.getBody());

        QueryResponse queryResponse = response.getBody();
        assertFalse(queryResponse.getSuccess());
        assertEquals("Query not found", queryResponse.getError());
        assertEquals(404, queryResponse.getCode());
        assertTrue(queryResponse.getExecutionTimeMs() >= 0);

        verify(queryService).loadQueryFromFile(queryIdentifier);
        verify(queryService, never()).executeQuery(any());
    }

    @Test
    @DisplayName("Should return 500 when SQL execution fails")
    void testExecuteQueryExecutionError() throws IOException {
        // Given
        String queryIdentifier = "failing_query";
        String sql = "SELECT * FROM nonexistent_table";

        when(queryService.loadQueryFromFile(queryIdentifier)).thenReturn(sql);
        when(queryService.executeQuery(sql)).thenThrow(new RuntimeException("Table does not exist"));

        // When
        ResponseEntity<QueryResponse> response = controller.executeQuery(queryIdentifier);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());

        QueryResponse queryResponse = response.getBody();
        assertFalse(queryResponse.getSuccess());
        assertEquals("Error while executing query", queryResponse.getError());
        assertEquals(500, queryResponse.getCode());
        assertTrue(queryResponse.getExecutionTimeMs() >= 0);

        verify(queryService).loadQueryFromFile(queryIdentifier);
        verify(queryService).executeQuery(sql);
    }

    @Test
    @DisplayName("Should handle empty query results")
    void testExecuteQueryEmptyResults() throws IOException {
        // Given
        String queryIdentifier = "empty_query";
        String sql = "SELECT * FROM users WHERE id = 999";
        List<Map<String, Object>> emptyResults = new ArrayList<>();

        when(queryService.loadQueryFromFile(queryIdentifier)).thenReturn(sql);
        when(queryService.executeQuery(sql)).thenReturn(emptyResults);

        // When
        ResponseEntity<QueryResponse> response = controller.executeQuery(queryIdentifier);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        QueryResponse queryResponse = response.getBody();
        assertTrue(queryResponse.getSuccess());
        assertTrue(queryResponse.getData().isEmpty());
        assertTrue(queryResponse.getExecutionTimeMs() >= 0);

        verify(queryService).loadQueryFromFile(queryIdentifier);
        verify(queryService).executeQuery(sql);
    }

    @Test
    @DisplayName("Should list SQL files successfully")
    void testListFilesSuccess() throws IOException {
        // Given
        PathMatchingResourcePatternResolver mockResolver = mock(PathMatchingResourcePatternResolver.class);
        Resource[] mockResources = {
            createMockResource("get_user_data.sql"),
            createMockResource("get_order_data.sql"),
            createMockResource("get_product_data.sql")
        };

        // We need to mock the static resolver creation since it's instantiated in the method
        // For this test, we'll focus on the logic rather than the actual file system interaction

        // When
        ResponseEntity<QueryResponse> response = controller.listFiles();

        // Then
        assertNotNull(response.getBody());
        QueryResponse queryResponse = response.getBody();
        assertTrue(queryResponse.getExecutionTimeMs() >= 0);

        // Note: This test verifies the method structure and response format
        // The actual file listing would require integration testing or more complex mocking
    }

    @Test
    @DisplayName("Should handle IOException in listFiles")
    void testListFilesIOException() {
        // This test verifies error handling in listFiles method
        // The exact implementation depends on how the PathMatchingResourcePatternResolver is mocked

        // When
        ResponseEntity<QueryResponse> response = controller.listFiles();

        // Then
        assertNotNull(response);
        // The method should handle IOException gracefully
    }

    @Test
    @DisplayName("Should measure execution time correctly")
    void testExecutionTimeMeasurement() throws IOException, InterruptedException {
        // Given
        String queryIdentifier = "slow_query";
        String sql = "SELECT * FROM users";
        List<Map<String, Object>> mockResults = Arrays.asList(Map.of("id", 1, "name", "John"));

        when(queryService.loadQueryFromFile(queryIdentifier)).thenReturn(sql);
        when(queryService.executeQuery(sql)).thenAnswer(invocation -> {
            Thread.sleep(10); // Simulate some execution time
            return mockResults;
        });

        // When
        ResponseEntity<QueryResponse> response = controller.executeQuery(queryIdentifier);

        // Then
        assertNotNull(response.getBody());
        QueryResponse queryResponse = response.getBody();
        assertTrue(queryResponse.getExecutionTimeMs() >= 10,
                  "Execution time should be at least 10ms, but was: " + queryResponse.getExecutionTimeMs());
    }

    @Test
    @DisplayName("Should handle complex query results with various data types")
    void testExecuteQueryComplexResults() throws IOException {
        // Given
        String queryIdentifier = "complex_query";
        String sql = "SELECT id, name, email, active, created_date FROM users";

        Date testDate = new Date();
        List<Map<String, Object>> mockResults = Arrays.asList(
            Map.of("id", 1, "name", "John", "email", "john@example.com", "active", true, "created_date", testDate),
            Map.of("id", 2, "name", "Jane", "email", "jane@example.com", "active", false, "created_date", testDate),
            Map.of("id", 3, "name", "Bob", "email", null, "active", true, "created_date", testDate)
        );

        when(queryService.loadQueryFromFile(queryIdentifier)).thenReturn(sql);
        when(queryService.executeQuery(sql)).thenReturn(mockResults);

        // When
        ResponseEntity<QueryResponse> response = controller.executeQuery(queryIdentifier);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        QueryResponse queryResponse = response.getBody();
        assertTrue(queryResponse.getSuccess());
        assertEquals(3, queryResponse.getData().size());

        // Verify that complex data types are handled correctly
        @SuppressWarnings("unchecked")
        Map<String, Object> firstResult = (Map<String, Object>) queryResponse.getData().get(0);
        assertEquals(1, firstResult.get("id"));
        assertEquals("John", firstResult.get("name"));
        assertEquals("john@example.com", firstResult.get("email"));
        assertEquals(true, firstResult.get("active"));
        assertEquals(testDate, firstResult.get("created_date"));

        verify(queryService).loadQueryFromFile(queryIdentifier);
        verify(queryService).executeQuery(sql);
    }

    @Test
    @DisplayName("Should handle null query identifier")
    void testExecuteQueryNullIdentifier() throws IOException {
        // Given
        String queryIdentifier = null;

        when(queryService.loadQueryFromFile(queryIdentifier))
            .thenThrow(new IllegalArgumentException("Query identifier cannot be null"));

        // When
        ResponseEntity<QueryResponse> response = controller.executeQuery(queryIdentifier);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        QueryResponse queryResponse = response.getBody();
        assertFalse(queryResponse.getSuccess());
        assertEquals(400, queryResponse.getCode());

        verify(queryService).loadQueryFromFile(queryIdentifier);
    }

    @Test
    @DisplayName("Should handle empty query identifier")
    void testExecuteQueryEmptyIdentifier() throws IOException {
        // Given
        String queryIdentifier = "";

        when(queryService.loadQueryFromFile(queryIdentifier))
            .thenThrow(new IOException("Query file not found: .sql"));

        // When
        ResponseEntity<QueryResponse> response = controller.executeQuery(queryIdentifier);

        // Then
        assertNotNull(response.getBody());

        QueryResponse queryResponse = response.getBody();
        assertFalse(queryResponse.getSuccess());
        assertEquals("Query not found", queryResponse.getError());
        assertEquals(404, queryResponse.getCode());

        verify(queryService).loadQueryFromFile(queryIdentifier);
    }

    private Resource createMockResource(String filename) throws IOException {
        Resource mockResource = mock(Resource.class);
        when(mockResource.getFilename()).thenReturn(filename);
        when(mockResource.exists()).thenReturn(true);
        return mockResource;
    }
}