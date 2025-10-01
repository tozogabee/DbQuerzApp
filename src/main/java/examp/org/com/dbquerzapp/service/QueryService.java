package examp.org.com.dbquerzapp.service;

import examp.org.com.dbquerzapp.validator.SqlValidator;
import examp.org.com.dbquerzapp.validator.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class QueryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SqlValidator sqlValidator;

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public String loadQueryFromFile(String queryIdentifier) throws IOException {
        String fileName = queryIdentifier + ".sql";
        log.info("Loading query from file: " + fileName);
        Resource resource = resolver.getResource("classpath:queries/" + fileName);
        log.info("Loading query from file: " + resource.getFilename());
        if (!resource.exists()) {
            log.error("Query file not found: " + fileName);
            throw new IOException("Query file not found: " + fileName);
        }

        log.info("Loading query from file: " + fileName);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    public ValidationResult validateSql(String sql) {
        log.info("Validating SQL: " + sql);
        return sqlValidator.validateSql(sql);
    }

    public List<Map<String, Object>> executeQuery(String sql) {
        log.info("Executing query: " + sql);
        ValidationResult validationResult = validateSql(sql);

        if (!validationResult.isValid()) {
            log.error("SQL validation failed: " + validationResult);
            throw new IllegalArgumentException("SQL validation failed: " + validationResult.getErrorMessage());
        }

        log.info("Executing query: " + sql);
        return jdbcTemplate.queryForList(sql);
    }
}