# DbQuerzApp

A Spring Boot application for executing pre-defined SQL queries with built-in validation and security features.

## Description

DbQuerzApp is a secure query execution service that allows you to execute pre-defined SQL SELECT queries stored as files. It includes comprehensive SQL validation to prevent SQL injection and restricts query execution to read-only operations.

### Key Features

- Execute SQL queries from pre-defined `.sql` files
- Comprehensive SQL validation with regex-based syntax checking
- SQL injection prevention
- Support for complex SELECT queries (WHERE, GROUP BY, HAVING, ORDER BY, LIMIT, UNION)
- PostgreSQL database with Liquibase migrations
- RESTful API with OpenAPI/Swagger documentation
- Connection pooling with HikariCP
- Docker support for easy deployment

## Technology Stack

- **Java**: 21
- **Spring Boot**: 3.5.6
- **Database**: PostgreSQL 16
- **Build Tool**: Gradle 8.14.3
- **ORM**: Hibernate/JPA
- **Migration**: Liquibase
- **API Documentation**: SpringDoc OpenAPI 3
- **Testing**: JUnit 5, Mockito
- **Code Coverage**: JaCoCo
- **Containerization**: Docker, Docker Compose

## Prerequisites

- Java 21 or higher
- Docker and Docker Compose
- Gradle 8.14.3+ (or use the included wrapper)

## Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd DbQuerzApp
```

### 2. Configure Environment Variables

Create a `.env` file in the project root:

```env
POSTGRES_DB=appdb
POSTGRES_USER=appuser
POSTGRES_PASSWORD=secret
PGADMIN_DEFAULT_EMAIL=admin@example.com
PGADMIN_DEFAULT_PASSWORD=admin
DB_HOST=localhost
DB_PORT=5432
```

### 3. Start the Database

Start PostgreSQL and pgAdmin using Docker Compose:

```bash
docker-compose up -d postgres pgadmin
```

Wait for the database to be healthy (check with `docker-compose ps`).

## Compilation

### Build with Gradle

```bash
./gradlew clean build
```

This will:
- Compile the source code
- Generate OpenAPI models
- Run all tests
- Generate JaCoCo coverage report
- Create the JAR file in `build/libs/`

### Build Docker Image

```bash
docker-compose build app
```

## Running the Application

### Option 1: Run Locally with Gradle

Set environment variables (or use defaults from `application.yml`):

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=appdb
export DB_USER=appuser
export DB_PASSWORD=secret
```

Run the application:

```bash
./gradlew bootRun
```

### Option 2: Run with Docker Compose

Start all services (database, pgAdmin, and application):

```bash
docker-compose up
```

Or run in detached mode:

```bash
docker-compose up -d
```

### Option 3: Run JAR Directly

```bash
java -jar build/libs/DbQuerzApp-0.0.1-SNAPSHOT.jar
```

The application will be available at `http://localhost:8080`

## API Documentation

Once the application is running, access the API documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## API Endpoints

### 1. List Available Query Files

```http
GET /api/execute-query/files
```

**Response:**
```json
{
  "success": true,
  "data": ["get_user_data.sql"],
  "execution_time_ms": 5
}
```

### 2. Execute Query

```http
GET /api/execute-query?query_identifier=get_user_data
```

**Response:**
```json
{
  "success": true,
  "data": [
    {"id": 1, "first_name": "Alice", "last_name": "Example", "age": 30, "email": "alice.example@test.com"},
    {"id": 2, "first_name": "Bob", "last_name": "Tester", "age": 25, "email": "bob.tester@test.com"}
  ],
  "execution_time_ms": 120
}
```

## Adding Custom Queries

1. Create a `.sql` file in `src/main/resources/queries/`
2. Write your SELECT query (only SELECT statements are allowed)
3. The file name (without `.sql`) becomes the `query_identifier`

Example (`src/main/resources/queries/get_active_users.sql`):

```sql
SELECT * FROM users WHERE age > 18
```

Execute it:

```http
GET /api/execute-query?query_identifier=get_active_users
```

## SQL Validation Rules

The application validates SQL queries to ensure security:

- Only SELECT statements are allowed
- No dangerous keywords (INSERT, UPDATE, DELETE, DROP, etc.)
- No SQL injection patterns (e.g., `WHERE 1=1`, `OR 'a'='a'`)
- Valid SQL syntax for SELECT queries
- Table/column names must start with letters or underscores
- Supports: WHERE, GROUP BY, HAVING, ORDER BY, LIMIT, UNION

## Testing

### Run All Tests

```bash
./gradlew test
```

### Run Tests with Coverage Report

```bash
./gradlew test jacocoTestReport
```

View the coverage report at: `build/reports/jacoco/test/html/index.html`

### Test Coverage Threshold

- Minimum coverage: 50%
- Per-class line coverage: 75%

Excluded from coverage:
- OpenAPI generated classes
- Main application class
- Configuration classes
- Exception handlers
- Model/Entity classes

## Database Management

### Access pgAdmin

URL: http://localhost:5050

Credentials:
- Email: `admin@example.com` (or value from `.env`)
- Password: `admin` (or value from `.env`)

### Connect to PostgreSQL

```bash
docker exec -it db-postgres psql -U appuser -d appdb
```

### View Liquibase Changelog

```sql
SELECT * FROM databasechangelog;
```

## Project Structure

```
DbQuerzApp/
├── src/
│   ├── main/
│   │   ├── java/examp/org/com/dbquerzapp/
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── service/         # Business logic
│   │   │   ├── validator/       # SQL validation
│   │   │   ├── exception/       # Exception handlers
│   │   │   └── dto/             # Data transfer objects
│   │   └── resources/
│   │       ├── application.yml          # Main configuration
│   │       ├── application-test.yml     # Test configuration
│   │       ├── db/changelog/            # Liquibase migrations
│   │       ├── queries/                 # SQL query files
│   │       └── openapi/openapi.yml      # API specification
│   └── test/                            # Unit and integration tests
├── docker-compose.yml                   # Docker services
├── Dockerfile                           # Application Docker image
├── build.gradle                         # Gradle build configuration
└── README.md
```

## Configuration

### Application Properties

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:appdb}
    username: ${DB_USER:appuser}
    password: ${DB_PASSWORD:secret}
    hikari:
      minimum-idle: 5
      maximum-pool-size: 10
      idle-timeout: 300000
      max-lifetime: 600000
```

### HikariCP Connection Pool

- Minimum idle connections: 5
- Maximum pool size: 10
- Idle timeout: 5 minutes
- Max connection lifetime: 10 minutes

## Troubleshooting

### PostgreSQL Driver Not Found

Ensure the PostgreSQL dependency is in `build.gradle`:

```gradle
implementation 'org.postgresql:postgresql'
```

### Liquibase Not Running

Check that:
- Database is running and accessible
- Environment variables are set correctly
- `spring.liquibase.enabled: true` in `application.yml`

### Connection Pool Issues

If connections are being created/closed on each request, verify HikariCP configuration is present in `application.yml`.

### Docker Build Fails

Ensure you have:
- Java 21 installed
- Gradle wrapper files present
- Correct paths in Dockerfile

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `./gradlew test`
5. Submit a pull request

## License

[Add your license information here]

## Contact

[Add your contact information here]
