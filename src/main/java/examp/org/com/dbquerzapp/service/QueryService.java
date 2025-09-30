package examp.org.com.dbquerzapp.service;

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

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public String loadQueryFromFile(String queryIdentifier) throws IOException {
        String fileName = queryIdentifier + ".sql";
        Resource resource = resolver.getResource("classpath:queries/" + fileName);

        if (!resource.exists()) {
            throw new IllegalArgumentException("Query file not found: " + fileName);
        }

        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    public boolean isValidSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        String trimmedSql = sql.trim().toUpperCase();

        // Allow only SELECT statements for security
        if (!trimmedSql.startsWith("SELECT")) {
            return false;
        }

        // Basic SQL injection protection
        String[] dangerousKeywords = {"DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE", "TRUNCATE", "EXEC", "EXECUTE"};
        for (String keyword : dangerousKeywords) {
            if (trimmedSql.contains(keyword)) {
                return false;
            }
        }

        return true;
    }

    public List<Map<String, Object>> executeQuery(String sql) {
        if (!isValidSql(sql)) {
            throw new IllegalArgumentException("Invalid or unsafe SQL query");
        }

        return jdbcTemplate.queryForList(sql);
    }
}