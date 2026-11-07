# NW Project Angular

[日本語の起動手順](README_ja.md)

## Run locally

Use JDK 21 and Docker with Linux containers. From the repository root, create `.env` from `.env.template` on first setup and set `DB_PASSWORD` and `MYSQL_ROOT_PASSWORD`. Keep your existing `.env` if it is already configured.

Prepare the private `jpa-security` dependency from an authorized checkout. Maven downloads Node.js, builds Angular, and packages the app for Tomcat. [Dependency setup](docs/birthday-encryption.md)

```bash
bash scripts/bootstrap-security.sh
./mvnw clean package
```

The Compose project name separates the Angular checkout's database volume. The container names and host ports in `.env.template` let the original `nwproject_docker` stack keep running. For an existing `.env`, add the five `NW_*` settings shown in the [Windows setup](README_ja.md).

```bash
docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml up -d db
docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml run --rm flyway
docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml up -d --build tomcat
```

After Flyway and Tomcat start successfully, open the [login page](http://localhost:8081/Login).

## Apply changes

Edit Angular components and their templates in `frontend/src/<feature>` and page entry HTML in `frontend/pages/<feature>`. Maven packages the entry pages under `WEB-INF/angular/<feature>` in the WAR.

```bash
./mvnw package
docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml restart tomcat
```

# Apply Database Migrations (Flyway)

Run from the repository root with `.env` and the Compose file explicitly specified. See [Database migrations](docs/database-migrations.md) for status checks and recovery guidance.

```bash
docker compose -p nwproject-angular --env-file .env -f .devcontainer/docker-compose.yml run --rm flyway
```

# Run Playwright Tests

```bash
npx playwright test
```

# Code Formatting (Prettier)

Install the project dependencies before running the formatter for the first time.

```bash
npm install
```

Format all supported JavaScript, CSS, Markdown, JSON, and YAML files:

```bash
npm run format
```

Check formatting without changing files:

```bash
npm run format:check
```

Format a specific file:

```bash
npx prettier --write "frontend/src/shared/login-context.ts"
```

# Start Flyway

Use the [database migration procedure](docs/database-migrations.md); do not remove the DB volume for a routine migration.

# VS Code Workspace Cache

```text
%APPDATA%\Code\User\workspaceStorage
```

# Tech Stack

-   Application: Angular 21, TypeScript, Java 21, and Servlet 4.0. Maven packages the WAR for Apache Tomcat 9.
-   Application libraries: MySQL Connector/J, Jackson, Lombok, and Spring Security's BCrypt password encoder.
-   Database: MySQL 8.0 with Flyway 11 SQL migrations.
-   Local runtime: Docker Compose with Nginx as the reverse proxy and TLS termination.
-   Optional AI integration: the separate My-AiCreate service, connected to Ollama through its API.
-   Optional article reader: the separate Qiita Guide (AI_Support) application, containerized with Node.js 22.
-   Quality checks: JUnit 5, Playwright for browser E2E tests, and Prettier for formatting.
-   CI: GitHub Actions builds the Angular and Java application, checks the WAR contents, applies Flyway migrations, and runs the browser tests. Birthday encryption has a separate integration workflow.
