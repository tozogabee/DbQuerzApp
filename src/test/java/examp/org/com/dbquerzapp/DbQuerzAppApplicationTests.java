package examp.org.com.dbquerzapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DbQuerzAppApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        // Check if data was inserted
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        System.out.println("Number of users in database: " + count);

        if (count > 0) {
            System.out.println("SUCCESS: Data was inserted!");
            jdbcTemplate.query("SELECT * FROM users", (rs) -> {
                System.out.println("User: " + rs.getString("first_name") + " " + rs.getString("last_name") + " - " + rs.getString("email"));
            });
        } else {
            System.out.println("PROBLEM: No data found in database");
        }
    }

}
