package examp.org.com.dbquerzapp.controller;

import com.example.api.ExecuteQueryApi;
import com.example.model.QueryResponse;
import examp.org.com.dbquerzapp.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class ExecuteQueryController implements ExecuteQueryApi {

    @Autowired
    private QueryService queryService;

    @Override
    public ResponseEntity<QueryResponse> executeQuery(String queryIdentifier) {
        long startTime = System.currentTimeMillis();
        try {

            long executionTime = System.currentTimeMillis() - startTime;

            // Load SQL from file
            String sql = queryService.loadQueryFromFile(queryIdentifier);

            // Execute the query
            List<Map<String, Object>> results = queryService.executeQuery(sql);


            // Convert results to List<Object>
            List<Object> data = new ArrayList<>(results);

            QueryResponse response = new QueryResponse();
            response.setSuccess(Boolean.TRUE);
            response.setData(data);
            response.setExecutionTimeMs(executionTime);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Query not found or invalid SQL
            long executionTime = System.currentTimeMillis() - startTime;
            //QueryResponse response = QueryResponse.failure(executionTime);
            QueryResponse response = new QueryResponse();
            response.setSuccess(Boolean.FALSE);
            response.setExecutionTimeMs(executionTime);
            response.setCode(400);
            return ResponseEntity.badRequest().body(response);

        } catch (IOException e) {
            // File reading error
            long executionTime = System.currentTimeMillis() - startTime;
            QueryResponse response = new QueryResponse();
            response.setSuccess(Boolean.FALSE);
            response.setError("Query not found");
            response.setCode(404);
            response.setExecutionTimeMs(executionTime);
            return ResponseEntity.ofNullable(response);

        } catch (Exception e) {
            // SQL execution error
            long executionTime = System.currentTimeMillis() - startTime;
            QueryResponse response = new QueryResponse();
            response.setSuccess(Boolean.FALSE);
            response.setError("Error while executing query");
            response.setCode(500);
            response.setExecutionTimeMs(executionTime);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Override
    public ResponseEntity<QueryResponse> listFiles()  {
        long startTime = System.currentTimeMillis();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:queries/*.sql");

            List<String> fileNames = new ArrayList<>();
            for (Resource resource : resources) {
                fileNames.add(resource.getFilename());
            }
            long executionTime = System.currentTimeMillis() - startTime;
            QueryResponse response = new QueryResponse();
            response.setSuccess(Boolean.TRUE);
            response.setExecutionTimeMs(executionTime);
            response.setData(new ArrayList<>(fileNames));
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            QueryResponse response = new QueryResponse();
            long executionTime = System.currentTimeMillis() - startTime;
            response.setSuccess(Boolean.FALSE);
            response.setError("Error while loading files");
            response.setCode(500);
            response.setExecutionTimeMs(executionTime);
            return ResponseEntity.notFound().build();
        }
    }
}
