# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**KONATABLOG** is a personal blog system built with Spring Boot. This is a fresh, greenfield project focused on creating a lightweight, full-featured blogging platform with markdown support, theme customization, and image hosting capabilities.

- **Type**: Spring Boot 3.5.7 application
- **Language**: Java 17
- **Build Tool**: Maven
- **Package**: `wiki.kana`
- **Development Timeline**: 2-3 weeks
- **Architecture**: Backend API (future frontend will be separate)

## Common Commands

### Build & Run
```bash
# Compile the project
./mvnw clean compile

# Run the application
./mvnw spring-boot:run

# Build JAR file
./mvnw clean package

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Testing
```bash
# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=KonatablogApplicationTests

# Run tests with verbose output
./mvnw test -X
```

### Code Quality
```bash
# Format code (if formatter is configured)
./mvnw formatter:format

# Run linting (if configured)
./mvnw spotbugs:check
```

### Database
```bash
# View H2 console (when H2 is configured)
# Navigate to: http://localhost:8080/h2-console

# Initialize database with migration scripts (when Flyway/Liquibase is configured)
./mvnw flyway:migrate
```

## Project Structure

This is a standard Spring Boot Maven project with minimal current structure:

```
src/
├── main/
│   ├── java/wiki/kana/
│   │   └── KonatablogApplication.java    # Main Spring Boot application
│   └── resources/
│       └── application.properties         # Application configuration
└── test/
    └── java/wiki/kana/
        └── KonatablogApplicationTests.java # Basic Spring context test

docs/
└── 需求文档.md                            # Requirements document (Chinese)

pom.xml                                    # Maven dependencies
```

## Architecture

### Current State
The project is in **initialization phase** with only:
- Spring Boot starter setup
- Basic application class
- Empty configuration file
- Test placeholder

### Planned Architecture (from requirements)

**Core Layers**:
- **Controller Layer** (`src/main/java/wiki/kana/controller/`): REST API endpoints
- **Service Layer** (`src/main/java/wiki/kana/service/`): Business logic
- **Repository Layer** (`src/main/java/wiki/kana/repository/`): Data access
- **Entity Layer** (`src/main/java/wiki/kana/entity/`): JPA entities
- **DTO Layer** (`src/main/java/wiki/kana/dto/`): Data transfer objects
- **Config Layer** (`src/main/java/wiki/kana/config/`): Security, CORS, etc.

**Planned Entities**:
- User (authentication & authorization)
- Post (blog posts with markdown)
- Category/Tags (blog organization)
- Media (image hosting)
- Settings (blog configuration)
- Theme (theme customization)

**Planned Features** (from `docs/需求文档.md`):
- JWT-based authentication
- Markdown blog authoring
- Image upload/management
- Theme system
- Guest vs Admin access modes
- SQLite database integration

## Development Status

**Current Phase**: Foundation Setup
- [ ] Add database dependencies (SQLite + JPA)
- [ ] Configure database connection
- [ ] Create entity models
- [ ] Set up authentication system
- [ ] Build REST API endpoints

**Next Steps Priority**:
1. Database configuration (SQLite + JPA + Hibernate)
2. Entity creation (User, Post, Tag, Media)
3. Authentication system (JWT)
4. Blog CRUD operations
5. Image upload functionality
6. Theme management

## Configuration Notes

### Database Configuration
When configuring SQLite, add to `application.properties`:
```properties
spring.datasource.url=jdbc:sqlite:konatablog.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
```

### JPA Properties
```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## Dependencies

**Current**:
- `spring-boot-starter-web`: Web MVC, REST, Jackson
- `spring-boot-starter-test`: JUnit 5, Mockito, Spring Test
- `lombok`: Reduces boilerplate code

**Planned Additions**:
- `spring-boot-starter-data-jpa`: ORM
- `sqlite-jdbc`: SQLite driver
- `hibernate-sqlite-dialect`: SQLite Hibernate dialect
- `spring-boot-starter-security`: Authentication
- `jjwt`: JWT token handling
- `spring-boot-starter-validation`: Input validation

## Important Project Details

1. **Package Naming**: All code should use `wiki.kana` package
2. **Java Version**: Java 17 minimum (uses sealed classes, records, etc.)
3. **Lombok**: Widely used - understand Lombok annotations (@Data, @Getter, @Setter, @Builder, etc.)
4. **Documentation**: Requirements in `docs/需求文档.md` (Chinese)
5. **IDE Support**: `.idea` folder tracked (IntelliJ IDEA)
6. **Git Status**: All initial project files are staged (see `gitStatus` in context)

## Getting Started

1. Review `docs/需求文档.md` for full feature requirements
2. Run `./mvnw clean compile` to verify setup
3. Add database dependencies to `pom.xml`
4. Start with entity creation
5. Build out repository layer
6. Implement services
7. Create REST controllers

## Key Files

- `pom.xml`: Maven configuration and dependencies
- `application.properties`: Configuration (currently minimal)
- `docs/需求文档.md`: Full feature requirements (Chinese)
- `src/main/java/wiki/kana/KonatablogApplication.java`: Application entry point
- `.gitignore`: IDE and build tool exclusions

## Development Approach

**Recommended Flow**:
1. **Incrementally build**: Start with database layer, move up
2. **Test first**: Add tests alongside new features
3. **Document**: Update `docs/需求文档.md` as features are implemented
4. **Small PRs**: Keep changes small and focused
5. **Validate early**: Test SQLite connection and JPA mapping immediately

This is a **clean slate project** - feel free to add necessary dependencies, create new packages, and implement features according to the requirements document.
