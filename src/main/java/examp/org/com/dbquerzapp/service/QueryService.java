package examp.org.com.dbquerzapp.service;

import examp.org.com.dbquerzapp.validator.SqlValidator;
import examp.org.com.dbquerzapp.validator.ValidationResult;
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
public class QueryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SqlValidator sqlValidator;

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public String loadQueryFromFile(String queryIdentifier) throws IOException {
        String fileName = queryIdentifier + ".sql";
        Resource resource = resolver.getResource("classpath:queries/" + fileName);

        if (!resource.exists()) {
            throw new IOException("Query file not found: " + fileName);
        }

        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    public ValidationResult validateSql(String sql) {
        return sqlValidator.validateSql(sql);
    }

    public List<Map<String, Object>> executeQuery(String sql) {
        ValidationResult validationResult = validateSql(sql);

        if (!validationResult.isValid()) {
            throw new IllegalArgumentException("SQL validation failed: " + validationResult.getErrorMessage());
        }

        return jdbcTemplate.queryForList(sql);
    }
}