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

# Run tests and skip if already passed
./mvnw test -DskipTests=false
```

### Code Quality
```bash
# Compile without running tests
./mvnw clean compile -DskipTests

# Run Spring Boot without tests
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Package application (includes tests)
./mvnw clean package -DskipTests
```

### Database Operations
```bash
# Database file location
data/konatablog.db

# Query SQLite database directly (if sqlite3 CLI installed)
sqlite3 data/konatablog.db

# View database schema
sqlite3 data/konatablog.db ".schema"

# List all tables
sqlite3 data/konatablog.db ".tables"

# Backup database
cp data/konatablog.db data/konatablog.db.backup
```

## Project Structure

This is a standard Spring Boot Maven project with the following structure:

```
src/
├── main/
│   ├── java/wiki/kana/
│   │   ├── KonatablogApplication.java     # Main Spring Boot application
│   │   ├── entity/                        # JPA Entity classes
│   │   │   ├── User.java
│   │   │   ├── Post.java
│   │   │   ├── Category.java
│   │   │   ├── Tag.java
│   │   │   ├── Media.java
│   │   │   ├── Themes.java
│   │   │   └── Settings.java
│   │   ├── repository/                    # Data access layer
│   │   │   └── MediaRepository.java
│   │   └── [planned] service/, controller/, dto/, config/
│   ├── resources/
│   │   ├── application.properties         # Application configuration
│   │   ├── static/                        # Static assets
│   │   └── templates/                     # Template files
│   └── test/
│       └── java/wiki/kana/
│           └── KonatablogApplicationTests.java # Basic Spring context test

data/                                      # SQLite database directory
docs/
└── 需求文档.md                            # Requirements document (Chinese)

pom.xml                                    # Maven dependencies
```

## Architecture

### Current State
The project has completed **initial foundation work**:
- ✅ Spring Boot starter setup with all core dependencies
- ✅ SQLite database configuration
- ✅ JPA/Hibernate with SQLite dialect configured
- ✅ All entity models created (User, Post, Category, Tag, Media, Settings, Themes)
- ✅ MediaRepository implementation
- 🔄 Service layer implementation (in progress)
- 🔄 REST API endpoints (planned)
- 🔄 Authentication system (planned)

### Architecture Layers

**Current Implementation**:
- **Entity Layer** (`src/main/java/wiki/kana/entity/`): JPA entities ✅
- **Repository Layer** (`src/main/java/wiki/kana/repository/`): Data access (MediaRepository ✅, others pending)

**Planned Layers**:
- **Service Layer** (`src/main/java/wiki/kana/service/`): Business logic
- **Controller Layer** (`src/main/java/wiki/kana/controller/`): REST API endpoints
- **DTO Layer** (`src/main/java/wiki/kana/dto/`): Data transfer objects
- **Config Layer** (`src/main/java/wiki/kana/config/`): Security, CORS, etc.

**Core Entities** (all created):
- `User.java` - Authentication & authorization
- `Post.java` - Blog posts with markdown content
- `Category.java` - Blog categories
- `Tag.java` - Blog tags
- `Media.java` - Image hosting
- `Settings.java` - Blog configuration
- `Themes.java` - Theme customization

**Planned Features** (from `docs/需求文档.md`):
- JWT-based authentication
- Markdown blog authoring
- Image upload/management
- Theme system
- Guest vs Admin access modes

## Development Status

**Current Phase**: Entity & Repository Layer Complete
- ✅ Add database dependencies (SQLite + JPA)
- ✅ Configure database connection
- ✅ Create entity models (all 7 entities)
- ✅ MediaRepository implementation
- 🔄 Complete repository layer (UserRepository, PostRepository, etc.)
- 🔄 Service layer implementation
- 🔄 REST API endpoints
- 🔄 Authentication system (JWT)
- 🔄 Blog CRUD operations

**Next Steps Priority**:
1. Complete repository layer for all entities
2. Implement service layer (UserService, PostService, MediaService, etc.)
3. Build REST API endpoints
4. JWT-based authentication system
5. Frontend integration (separate repository)
6. Image upload functionality
7. Theme management

## Configuration Notes

### Database Configuration
Database is already configured in `application.properties`:
- **Location**: `data/konatablog.db`
- **Dialect**: SQLite with Hibernate support
- **Auto-DDL**: Enabled (update mode)

### Application Properties
```properties
# Database
spring.datasource.url=jdbc:sqlite:/data/konatablog.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## Dependencies

**All Core Dependencies Added**:
- ✅ `spring-boot-starter-web`: Web MVC, REST, Jackson
- ✅ `spring-boot-starter-test`: JUnit 5, Mockito, Spring Test
- ✅ `lombok`: Reduces boilerplate code
- ✅ `spring-boot-starter-data-jpa`: ORM
- ✅ `sqlite-jdbc` (v3.46.1.0): SQLite driver
- ✅ `hibernate-community-dialects` (v6.5.2.Final): SQLite Hibernate dialect
- ✅ `spring-boot-starter-security`: Authentication
- ✅ `jjwt` (v0.12.5): JWT token handling
- ✅ `spring-boot-starter-validation`: Input validation

## Common Development Tips

**Lombok Setup**:
- Ensure annotation processing is enabled in your IDE
- IntelliJ IDEA: Settings → Build → Compiler → Annotation Processors → Enable
- Lombok generates getters, setters, constructors, builders automatically

**Database Tips**:
- Database auto-creates tables on first run (DDL auto=update)
- Check console logs for DDL statements (enabled in application.properties)
- SQLite stores data in `data/konatablog.db` file
- Backup database before schema changes

**Entity Development**:
- All entities use JPA annotations (@Entity, @Id, @ManyToOne, etc.)
- Lombok reduces boilerplate (@Data, @Getter, @Setter, @Builder)
- Review entity relationships in Entity Relationship Overview section
- Test entities by running application and checking table creation

**Spring Data JPA**:
- Extend JpaRepository for standard CRUD operations
- Custom query methods follow Spring naming conventions (findByTitle, etc.)
- Use @Query annotation for complex queries

## Important Project Details

1. **Package Naming**: All code should use `wiki.kana` package
2. **Java Version**: Java 17 minimum (uses sealed classes, records, etc.)
3. **Lombok**: Widely used - understand Lombok annotations (@Data, @Getter, @Setter, @Builder, etc.)
4. **Documentation**: Requirements in `docs/需求文档.md` (Chinese)
5. **IDE Support**: `.idea` folder tracked (IntelliJ IDEA)
6. **Git Status**: All initial project files are staged (see `gitStatus` in context)

## Getting Started

1. Review `docs/需求文档.md` for full feature requirements
2. Run `./mvnw clean compile` to verify setup and dependencies
3. Database is already configured - start the app to auto-create tables
4. Entity models are already created in `src/main/java/wiki/kana/entity/`
5. Build out remaining repositories (UserRepository, PostRepository, etc.)
6. Implement service layer
7. Create REST controllers

## Key Files

**Configuration**:
- `pom.xml`: Maven configuration and dependencies
- `application.properties`: Database and JPA configuration

**Core Application**:
- `src/main/java/wiki/kana/KonatablogApplication.java`: Application entry point

**Entity Layer** (all JPA entities with Lombok):
- `entity/User.java`: User authentication & authorization
- `entity/Post.java`: Blog posts with markdown content
- `entity/Category.java`: Blog categories
- `entity/Tag.java`: Blog tags
- `entity/Media.java`: Image hosting
- `entity/Settings.java`: Blog configuration
- `entity/Themes.java`: Theme customization

**Repository Layer**:
- `repository/MediaRepository.java`: Media data access (implemented)

**Documentation**:
- `docs/需求文档.md`: Full feature requirements (Chinese)

**Other**:
- `.gitignore`: IDE and build tool exclusions
- `data/`: SQLite database directory

## Entity Relationship Overview

**Core Entities**:
- **User**: 1:many Posts, 1:1 Settings
- **Post**: many:many Tags, many:1 User, many:1 Category
- **Category**: 1:many Posts
- **Tag**: many:many Posts
- **Media**: Uploaded files/images
- **Settings**: Blog configuration (1:1 User)
- **Themes**: Theme customization

All entities use JPA annotations and Lombok for reduced boilerplate code.

## Development Approach

**Recommended Flow**:
1. **Complete repository layer**: Create repositories for all entities except MediaRepository
2. **Service layer**: Implement business logic in service classes
3. **Controller layer**: Build REST API endpoints
4. **Authentication**: Integrate JWT-based security
5. **Test first**: Add tests alongside new features
6. **Document**: Update this CLAUDE.md as architecture evolves
7. **Small PRs**: Keep changes small and focused

**Validation Steps**:
1. Run `./mvnw clean compile` - verify no compilation errors
2. Run `./mvnw spring-boot:run` - verify SQLite connection and table creation
3. Check `data/konatablog.db` is created
4. Verify JPA schema matches entities (check logs for DDL statements)

This is a **growing project** - entities and repositories are in place, ready for service and controller implementation.
