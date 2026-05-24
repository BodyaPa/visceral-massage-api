# Visceral Massage API

Backend for Ataraksia: Java 21, Spring Boot, PostgreSQL, Flyway and Spring
Security.

## Requirements

- Java 21
- Docker Compose for local infrastructure

## Local Development

The default active profile is `dev`. It expects:

- API at `http://localhost:8080`;
- PostgreSQL at `localhost:5432`, database `visceral`, user `visceral`;
- Mailpit SMTP at `127.0.0.1:1025`.

Start the Compose services:

```bash
docker compose up -d
```

Important: the current PostgreSQL entry in `compose.yaml` uses port `3080`
and different database credentials than `application-dev.yml`. Until that
configuration is aligned, provide a PostgreSQL instance matching the `dev`
profile or adjust local configuration outside committed secrets.

Run the API:

```bash
./gradlew bootRun
```

## Checks

```bash
./gradlew --no-daemon test
docker compose -f compose.yaml config --quiet
```

## Local Email Catcher

The `dev` profile sends application email through Mailpit instead of a real
email provider.

Mailpit accepts SMTP messages on `127.0.0.1:1025` and exposes its local Web UI
at `http://127.0.0.1:8025`. Both ports are bound to localhost only.

To select the profile explicitly:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

## Sensitive Configuration

Do not commit JWT secrets, admin bootstrap credentials, SMTP credentials or
environment files. Production settings must be supplied through environment
variables or deployment secrets.
