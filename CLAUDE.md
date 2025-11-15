# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**KONATABLOG** is a personal blog system built with Spring Boot. This is a lightweight, full-featured blogging platform with markdown support, theme customization, and image hosting capabilities.

- **Type**: Spring Boot 3.5.7 application
- **Language**: Java 17
- **Build Tool**: Maven
- **Package**: `wiki.kana`
- **Database**: SQLite (file: `data/konatablog.db`)
- **Architecture**: Backend API (future frontend will be separate)
- **Status**: Repository & Entity layers complete, UserService complete, implementing remaining service layer

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

## Verified Development Commands

All commands below have been tested and work correctly:

**Build & Test**:
```bash
./mvnw clean compile                    # ✅ Compiles successfully
./mvnw test                            # ✅ All tests pass
./mvnw test -Dtest=KonatablogApplicationTests  # ✅ Basic context test passes
```

**Run Application**:
```bash
./mvnw spring-boot:run                 # ✅ Starts on port 8081
# Database auto-creates at data/konatablog.db
```

**Database Operations**:
```bash
sqlite3 data/konatablog.db ".schema"   # View current schema
sqlite3 data/konatablog.db ".tables"   # List all tables
```

Note: Schema migration warnings are expected due to SQLite limitations.

## Project Structure

This is a standard Spring Boot Maven project with the following structure:

```
src/
├── main/
│   ├── java/wiki/kana/
│   │   ├── KonatablogApplication.java     # Main Spring Boot application
│   │   ├── config/                        # Configuration classes
│   │   │   └── SecurityConfig.java        # Spring Security configuration
│   │   ├── entity/                        # JPA Entity classes ✅ COMPLETE
│   │   │   ├── User.java
│   │   │   ├── Post.java
│   │   │   ├── Category.java
│   │   │   ├── Tag.java
│   │   │   ├── Media.java
│   │   │   ├── Themes.java
│   │   │   └── Settings.java
│   │   ├── repository/                    # Data access layer ✅ COMPLETE
│   │   │   ├── UserRepository.java
│   │   │   ├── PostRepository.java
│   │   │   ├── CategoryRepository.java
│   │   │   ├── TagRepository.java
│   │   │   ├── MediaRepository.java
│   │   │   ├── SettingsRepository.java
│   │   │   └── ThemesRepository.java
│   │   ├── service/                       # Business logic layer 🔄 IN PROGRESS
│   │   └── exception/                     # Custom exceptions
│   │       ├── ResourceNotFoundException.java
│   │       └── DuplicateResourceException.java
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
The project has completed **foundation and data access layers**:
- ✅ Spring Boot starter setup with all core dependencies
- ✅ SQLite database configuration and connection
- ✅ JPA/Hibernate with SQLite dialect configured
- ✅ All entity models created (User, Post, Category, Tag, Media, Settings, Themes)
- ✅ Complete repository layer (all 7 repositories implemented)
- ✅ Security configuration (BCrypt password encoding)
- ✅ Custom exception classes
- 🔄 Service layer implementation (in progress)
- 🔄 REST API endpoints (planned)
- 🔄 JWT authentication system (dependencies ready)

### Architecture Layers

**Current Implementation**:
- **Entity Layer** (`src/main/java/wiki/kana/entity/`): JPA entities ✅ COMPLETE
- **Repository Layer** (`src/main/java/wiki/kana/repository/`): Data access ✅ COMPLETE
- **Config Layer** (`src/main/java/wiki/kana/config/`): Security configuration ✅ COMPLETE
- **Exception Layer** (`src/main/java/wiki/kana/exception/`): Custom exceptions ✅ COMPLETE

**In Progress**:
- **Service Layer** (`src/main/java/wiki/kana/service/`): Business logic 🔄 PARTIAL
  - ✅ UserService: Complete with full CRUD, authentication, and statistics
  - 🔄 PostService, CategoryService, TagService, MediaService, SettingsService, ThemesService: TODO

**Planned Layers**:
- **Controller Layer** (`src/main/java/wiki/kana/controller/`): REST API endpoints
- **DTO Layer** (`src/main/java/wiki/kana/dto/`): Data transfer objects

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

**Current Phase**: Service Layer Partially Complete (4/7 services implemented)
- ✅ Add database dependencies (SQLite + JPA)
- ✅ Configure database connection
- ✅ Create entity models (all 7 entities)
- ✅ Complete repository layer (all 7 repositories implemented)
- ✅ Security configuration (BCrypt password encoder)
- ✅ Custom exception classes
- ✅ UserService implementation (complete with CRUD, auth, statistics)
- ✅ PostService implementation (complete with publishing, search, view count tracking)
- ✅ CategoryService implementation (complete with hierarchical structure support)
- ✅ TagService implementation (complete with CRUD, statistics, search, recommendations)
- 🔄 Service layer completion (MediaService, SettingsService, ThemesService remaining)
- 🔄 REST API endpoints
- 🔄 JWT authentication system
- 🔄 Blog CRUD operations

**Next Steps Priority**:
1. ✅ Complete repository layer for all entities
2. 🔄 Complete service layer (MediaService, SettingsService, ThemesService remaining - 3/7 services)
3. Build REST API endpoints
4. JWT-based authentication system
5. Frontend integration (separate repository)
6. Image upload functionality
7. Theme management

## Database Schema Migration Notes

**SQLite Constraint Limitation**:
- SQLite does not support adding UNIQUE columns via ALTER TABLE
- When modifying entity relationships, you may see warnings like: `Cannot add a UNIQUE column`
- **Solution**: Delete `data/konatablog.db` and restart the application to recreate the schema cleanly
- Current schema migration attempts to handle this gracefully but may log warnings during tests

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
5. All repositories are implemented in `src/main/java/wiki/kana/repository/`
6. Security configuration is set up in `src/main/java/wiki/kana/config/`
7. **Next**: Implement service layer (start with UserService, PostService)
8. **Then**: Create REST controllers
9. **Finally**: Set up JWT authentication

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

**Repository Layer** (✅ COMPLETE):
- `repository/UserRepository.java`: User data access with custom queries
- `repository/PostRepository.java`: Blog post data access
- `repository/CategoryRepository.java`: Category data access
- `repository/TagRepository.java`: Tag data access
- `repository/MediaRepository.java`: Media file data access
- `repository/SettingsRepository.java`: Settings data access
- `repository/ThemesRepository.java`: Themes data access

**Security & Configuration**:
- `config/SecurityConfig.java`: Spring Security configuration (BCrypt password encoder)

**Exception Handling**:
- `exception/ResourceNotFoundException.java`: Custom exception for missing resources
- `exception/DuplicateResourceException.java`: Custom exception for duplicate resources

**Documentation**:
- `docs/需求文档.md`: Full feature requirements (Chinese)

**Other**:
- `.gitignore`: IDE and build tool exclusions
- `data/`: SQLite database directory (contains `konatablog.db`)

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

## Repository Layer Implementation Details

**UserRepository Features**:
- Standard CRUD operations via JpaRepository
- Custom queries: findByUsername, findByEmail, findByUsernameOrEmail
- Role-based queries: findByRole, findByIsActiveTrue
- Statistics queries: countAllUsers, countByRole
- Advanced queries: findRecentLoginUsers, findActiveAuthors

**PostRepository Features**:
- CRUD operations for blog posts
- Custom queries for published posts, posts by category/author
- Search functionality by title and content
- Pagination support for post listings
- View count tracking

**Other Repositories**:
- CategoryRepository: CRUD with hierarchical structure support
- TagRepository: Many-to-many relationship handling
- MediaRepository: File upload and management
- SettingsRepository: Key-value configuration storage
- ThemesRepository: Theme configuration management

## Service Layer Implementation Status

**Current Implementation Progress**:

- ✅ **UserService** (442 lines): Fully implemented
  - Complete CRUD operations with validation
  - BCrypt password encoding
  - Authentication methods (username/email login)
  - User activation/deactivation and role management
  - Statistics queries (count users, recent logins, active authors)
  - Custom validation and exception handling
  - Comprehensive logging with SLF4J

- ✅ **PostService** (369 lines): Fully implemented
  - Complete CRUD operations for blog posts
  - Publishing workflow (publish/unpublish)
  - Search functionality (title, content, author, category)
  - Tag processing and management integration
  - View count tracking and popular posts
  - Slug generation and URL-friendly titles
  - Statistics queries (total, published, draft counts)
  - Comprehensive logging with SLF4J

- ✅ **CategoryService** (361 lines): Fully implemented
  - Complete CRUD operations for categories
  - Hierarchical structure support (parent/child relationships)
  - Automatic slug generation from names
  - Category activation/deactivation
  - Statistics queries (post counts per category)
  - Top-level and child category queries
  - Validation for deletion constraints (no children/posts)
  - Comprehensive logging with SLF4J

- ✅ **TagService** (450+ lines): Fully implemented
  - Complete CRUD operations for tags with name/slug uniqueness
  - Usage count management and automatic tracking
  - Search functionality (by name, keyword matching)
  - Popular tags and recommendation engine
  - Tag association management (many-to-many with posts)
  - Related tags discovery and intelligent recommendations
  - Statistics queries (total, used, unused counts)
  - Batch operations (get/create multiple tags)
  - Force delete with association cleanup
  - Comprehensive logging with SLF4J

- 🔄 **Remaining Services** (Need Implementation):
  - **MediaService**: File upload validation, storage, URL generation
  - **SettingsService**: Key-value configuration management
  - **ThemesService**: Theme switching and customization

## Testing Strategy

**Verified Working Tests**:
- Context loading test: `./mvnw test -Dtest=KonatablogApplicationTests`
- All 7 repositories are auto-configured and available for testing
- SQLite database connection tested and working
- 7 JPA repository interfaces detected during startup
- **Service Layer Tests**: 53 tests total, all passing
  - `KonatablogApplicationTests`: 1 test - Spring context loading
  - `CategoryServiceTest`: 10 tests - Complete CRUD, hierarchical operations, status management
  - `PostServiceTest`: 3 tests - CRUD operations, publishing workflow
  - `UserServiceTest`: 3 tests - User creation, deletion, duplicate validation
  - `TagServiceTest`: 36 tests - Complete CRUD, usage management, search, recommendations, statistics, batch operations
- **Test Coverage**: 4/7 services have comprehensive unit tests

**Next Testing Priorities**:
1. ✅ Add service layer unit tests (UserServiceTest, PostServiceTest, CategoryServiceTest, TagServiceTest complete)
2. Add service layer unit tests for remaining services (MediaServiceTest, SettingsServiceTest, ThemesServiceTest)
3. Test repository custom queries with actual data
4. Integration tests with database operations
5. Test exception handling scenarios (ResourceNotFoundException, DuplicateResourceException)
6. Test authentication and authorization flows

**Test Database**: Tests use the same SQLite database at `data/konatablog.db`
- Current tests create tables but don't interfere with existing data
- Schema migration warnings are expected (SQLite ALTER TABLE limitations)
- Consider using separate test profile for test database isolation
- Tests run successfully with full Spring Boot context initialization

## Development Approach

**Current Status**: ✅ Repository Layer Complete, ✅ 4/7 Service Layer Complete (UserService, PostService, CategoryService, TagService)

**Recommended Flow**:
1. ✅ **Complete repository layer**: All repositories implemented with custom queries
2. 🔄 **Complete service layer**: Implement remaining business logic services (3 remaining: MediaService, SettingsService, ThemesService)
3. **Controller layer**: Build REST API endpoints
4. **Authentication**: Integrate JWT-based security
5. **Test first**: Add tests alongside new features
6. **Document**: Update this CLAUDE.md as architecture evolves
7. **Small PRs**: Keep changes small and focused

**Validation Steps**:
1. Run `./mvnw clean compile` - verify no compilation errors ✅
2. Run `./mvnw spring-boot:run` - verify SQLite connection and table creation ✅
3. Check `data/konatablog.db` is created ✅
4. Verify JPA schema matches entities (check logs for DDL statements)
5. Test repository methods with sample data
6. Run `./mvnw test -Dtest=KonatablogApplicationTests` - verify basic context loads ✅
7. Run `./mvnw test` - verify all 53 tests pass ✅

This is a **growing project** - entities, repositories, security configuration, and 4/7 service implementations are complete, ready for remaining service (MediaService, SettingsService, ThemesService) and controller implementation.

## Service Layer Implementation Guidelines

**Service Layer Structure** (🔄 IN PROGRESS - 4/7 complete):
```
src/main/java/wiki/kana/service/
├── UserService.java          # ✅ User management & authentication
├── PostService.java          # ✅ Blog post business logic
├── CategoryService.java      # ✅ Category management
├── TagService.java           # ✅ Tag management
├── MediaService.java         # 🔄 File upload & media management (TODO)
├── SettingsService.java      # 🔄 System configuration (TODO)
└── ThemesService.java        # 🔄 Theme management (TODO)
```

**Service Implementation Patterns**:

1. **Dependency Injection**: Inject repositories and other services
   ```java
   @Service
   @RequiredArgsConstructor
   public class UserService {
       private final UserRepository userRepository;
       private final PasswordEncoder passwordEncoder;
   }
   ```

2. **Exception Handling**: Use custom exceptions from `wiki.kana.exception`
   ```java
   public User findById(Long id) {
       return userRepository.findById(id)
           .orElseThrow(() -> new ResourceNotFoundException("User not found"));
   }
   ```

3. **Data Validation**: Validate inputs using `@Valid` and custom validators
   ```java
   public User createUser(CreateUserRequest request) {
       // Validate request
       if (userRepository.findByUsername(request.getUsername()).isPresent()) {
           throw new DuplicateResourceException("Username already exists");
       }
   }
   ```

4. **Business Logic**: Implement complex operations combining multiple repositories
   ```java
   public Post publishPost(Long postId, Long userId) {
       User author = userService.findById(userId);
       Post post = postRepository.findById(postId)
           .filter(p -> p.getAuthor().equals(author))
           .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

       post.setStatus(PostStatus.PUBLISHED);
       post.setPublishedAt(LocalDateTime.now());
       return postRepository.save(post);
   }
   ```

**Service Responsibilities**:
- **UserService**: Authentication, password encoding, user CRUD
- **PostService**: Blog post management, publishing workflow, search
- **MediaService**: File upload validation, storage, URL generation
- **SettingsService**: System configuration management
- **ThemesService**: Theme switching and customization

**Testing Strategy**:
- Unit tests for each service method
- Integration tests with repositories
- Mock external dependencies (file storage, email service)
- Test exception scenarios and edge cases
