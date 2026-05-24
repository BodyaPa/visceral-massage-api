# Visceral Massage API

## Local Email Catcher

The `dev` profile sends application email through Mailpit instead of a real
email provider.

Start the local Docker Compose services:

```bash
docker compose up -d
```

Mailpit accepts SMTP messages on `127.0.0.1:1025` and exposes its local Web UI
at `http://127.0.0.1:8025`. Both ports are bound to localhost only.

Run the application with the `dev` profile to use this SMTP configuration:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```
