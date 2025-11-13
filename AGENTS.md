# Repository Guidelines

## Project Structure & Module Organization
- src/main/java/wiki/kana: core code (entity, repository, service, config, exception); entry point KonatablogApplication.java.
- src/main/resources: application.properties.
- src/test/java: unit/integration tests (KonatablogApplicationTests.java, UserServiceTest.java).
- data: SQLite DB data/konatablog.db.
- docs: docs (docs/需求文档.md).

## Build, Test, and Development Commands
- Use Git Bash for all commands.
- Build: ./mvnw clean package
- Run (dev): ./mvnw spring-boot:run
- Run (jar): java -jar target/konatablog-0.0.1-SNAPSHOT.jar
- Tests: ./mvnw test

## Coding Style & Naming Conventions
- Java 17; 4-space indentation, no tabs.
- Packages lower-case; classes PascalCase; methods/fields camelCase; constants UPPER_SNAKE_CASE.
- Use Lombok when present (@Slf4j, @RequiredArgsConstructor).
- Suffix: repositories Repository, services Service; entity names are singular.

## Testing Guidelines
- Framework: JUnit 5 via spring-boot-starter-test.
- Place tests under src/test/java; name *Test.java or *Tests.java.
- Unit-test services; @DataJpaTest for repositories; @SpringBootTest for application-level tests.
- Avoid relying on data/konatablog.db; setup/teardown data in tests.

## Commit & Pull Request Guidelines
- Use Conventional Commits: feat, fix, docs, refactor, test, chore.
- Keep commits small and runnable; explain intent when not obvious.
- PRs: clear description, linked issues (Closes #123), tests or verification steps, and notes on config/schema changes.

## Security & Configuration Tips
- App port 8081; DB configured in src/main/resources/application.properties.
- For repo-relative dev DB: spring.datasource.url=jdbc:sqlite:data/konatablog.db.
- Never commit secrets or tokens.

## Agent-Specific Instructions
- Use Git Bash exclusively for all terminal calls. Avoid PowerShell/cmd; run commands from the repo root.
