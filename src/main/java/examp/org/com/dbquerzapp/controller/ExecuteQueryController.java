package examp.org.com.dbquerzapp.controller;

import com.example.api.ExecuteQueryApi;
import com.example.model.QueryResponse;
import examp.org.com.dbquerzapp.service.QueryService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ExecuteQueryController implements ExecuteQueryApi {

    @Autowired
    private QueryService queryService;

    @Override
    public ResponseEntity<QueryResponse> executeQuery(String queryIdentifier) {
        long startTime = System.currentTimeMillis();
        log.info("Executing query: " + queryIdentifier);
        log.info("start time: "+startTime);
        try {
            String sql = queryService.loadQueryFromFile(queryIdentifier);
            log.info("sql: "+sql);
            List<Map<String, Object>> results = queryService.executeQuery(sql);
            log.info("Query result: "+results);
            List<Object> data = new ArrayList<>(results);
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Execution time: "+executionTime);
            QueryResponse response = new QueryResponse();
            response.setSuccess(Boolean.TRUE);
            response.setData(data);
            response.setExecutionTimeMs(executionTime);
            log.info("Status code: 200");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            QueryResponse response = new QueryResponse();
            response.setSuccess(Boolean.FALSE);
            response.setExecutionTimeMs(null);
            response.setData(null);
            response.setError(e.getMessage());
            response.setCode(400);
            log.info("Status code: 400");
            log.info("Execution time: "+executionTime);
            return ResponseEntity.badRequest().body(response);

        } catch (IOException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            QueryResponse response = new QueryResponse();
            response.setSuccess(Boolean.FALSE);
            response.setError("Query not found");
            response.setCode(404);
            response.setExecutionTimeMs(null);
            response.setData(null);
            log.info("Status code: 404");
            log.info("Execution time: "+executionTime);
            return ResponseEntity.ofNullable(response);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            QueryResponse response = new QueryResponse();
            response.setSuccess(Boolean.FALSE);
            response.setError("Error while executing query");
            response.setCode(500);
            response.setExecutionTimeMs(null);
            response.setData(null);
            log.info("Status code: 500");
            log.info("Execution time: "+executionTime);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Override
    public ResponseEntity<QueryResponse> listFiles()  {
        log.info("Executing listFiles");
        long startTime = System.currentTimeMillis();
        log.info("start time: "+startTime);
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:queries/*.sql");
            log.info("resources: "+resources);
            List<String> fileNames = new ArrayList<>();
            for (Resource resource : resources) {
                fileNames.add(resource.getFilename());
            }
            long executionTime = System.currentTimeMillis() - startTime;
            QueryResponse response = new QueryResponse();
            response.setSuccess(Boolean.TRUE);
            response.setExecutionTimeMs(executionTime);
            response.setData(new ArrayList<>(fileNames));
            log.info("Status code: 200");
            log.info("Execution time: "+executionTime);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            QueryResponse response = new QueryResponse();
            long executionTime = System.currentTimeMillis() - startTime;
            response.setSuccess(Boolean.FALSE);
            response.setError("Error while loading files");
            response.setCode(500);
            response.setExecutionTimeMs(null);
            response.setData(null);
            log.info("Status code: 500");
            log.info("Execution time: "+executionTime);
            return ResponseEntity.notFound().build();
        }
    }
}
