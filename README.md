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

## Browser Authentication Security

The API authenticates browser requests with `HttpOnly` access and refresh
cookies. Unsafe requests also require a CSRF token:

1. Call `GET /api/auth/csrf` with credentials enabled.
2. Read the returned JSON `token` value.
3. Send that value in the `X-XSRF-TOKEN` header on `POST`, `PUT`, `PATCH` and
   `DELETE` requests while allowing the browser to send cookies.

The `XSRF-TOKEN` cookie is `HttpOnly`; application code should use the JSON
token, not read cookies. Registration passwords must contain at least 12
characters, including uppercase and lowercase letters, a number and a special
character. Allowed browser origins are configured through
`app.cors.allowed-origins`; staging and production take values from
`CORS_ALLOWED_ORIGINS`.

Registration stores a validated first and last name and requires at least one
contact method: phone or email. Login accepts one `identifier` field containing
either value and returns the same invalid-credentials response for failed
attempts. Ukrainian phone values accept either canonical `+380...` or local
`0XXXXXXXXX` input; local input is normalized to canonical storage.

## Local Email Catcher

The `dev` profile sends application email through Mailpit instead of a real
email provider.

Mailpit accepts SMTP messages on `127.0.0.1:1025` and exposes its local Web UI
at `http://127.0.0.1:8025`. Both ports are bound to localhost only.

To select the profile explicitly:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

## Staging Email Catcher

The `staging` profile keeps production-like database, cookie and CORS settings,
but sends application email only to the Mailpit service on the private Compose
network. It does not expose a staging email test API.

Start staging from `deploy/` using the production base file and the staging
override:

```bash
docker compose -f docker-compose.prod.yml -f docker-compose.staging.yml up -d --build
```

Set database, JWT and CORS values through deployment secrets as for production.
`STAGING_MAIL_FROM` can override the non-production sender address.

Mailpit SMTP is not published from the staging host. Its Web UI is bound only
to `127.0.0.1` on the host, on port `8025` by default. Inspect captured email
from an administrator workstation through an SSH tunnel:

```bash
ssh -L 8025:127.0.0.1:8025 user@staging-host
```

Then open `http://127.0.0.1:8025` locally. Do not reverse-proxy this UI as a
public staging route.

## Sensitive Configuration

Do not commit JWT secrets, owner bootstrap credentials, SMTP credentials or
environment files. Production settings must be supplied through environment
variables or deployment secrets.

## News Authoring Lifecycle

News items use `DRAFT`, `PUBLISHED` and `ARCHIVED` status values. `POST
/api/admin/news` creates a `DRAFT` immediately, including when the request
body is empty, so media can be attached before text is written.

CSRF-protected content-management actions:

- `POST /api/admin/news/{id}/publish`;
- `POST /api/admin/news/{id}/unpublish`;
- `POST /api/admin/news/{id}/archive`;
- `POST /api/admin/news/{id}/restore`.
- `DELETE /api/admin/news/{id}` removes only a never-published `DRAFT`; it is
  not a deletion path for historical published or archived news.

Only `PUBLISHED` news is returned from public endpoints, and only when the
requested `lang=ua|en` translation has both title and content. Public reads do
not fall back to the other translation.

## Private Media Storage And Covers

Uploaded assets are stored privately and public access is granted only after
an SMM/editor account links an asset to a published news item.

SMM/editor accounts can use CSRF-protected endpoints:

- `POST /api/admin/media` with multipart part `file` to upload an asset;
- `GET /api/admin/media` and `GET /api/admin/media/{id}` for metadata;
- `GET /api/admin/media/{id}/content` for a protected preview;
- `DELETE /api/admin/media/{id}` to remove metadata and stored bytes.

An uploaded asset remains private until it is linked to one news item:

- `PUT /api/admin/news/{newsId}/media/{mediaId}` links a private asset;
- `GET /api/admin/news/{newsId}/media` lists linked assets for editing;
- `DELETE /api/admin/news/{newsId}/media/{mediaId}` detaches an asset.
- `PUT /api/admin/news/{newsId}/cover/{mediaId}` selects a linked image as cover;
- `PUT /api/admin/news/{newsId}/cover/display-mode/{FILL|FIT}` selects either
  a cropped cover/hero treatment or an uncropped screenshot/vertical treatment;
- `DELETE /api/admin/news/{newsId}/cover` clears its cover.

Once linked, its public content is available only through
`GET /api/news/{newsId}/media/{mediaId}/content` while that news item is
`PUBLISHED`. A linked asset cannot be
deleted directly; it must be detached first. Deleting its news item detaches
the asset through the database relationship, leaving the stored asset private
for explicit cleanup or reuse.

The service stores metadata in PostgreSQL and local file bytes under
`./var/media` in development. The directory is ignored by Git. Accepted types
are JPEG, PNG, WebP, MP4 and WebM; requests are checked against both their
declared content type and the file signature. The default application limit is
25 MB and can be changed through `MEDIA_MAX_FILE_SIZE_BYTES` together with
`MEDIA_MULTIPART_MAX_FILE_SIZE`/`MEDIA_MULTIPART_MAX_REQUEST_SIZE`.

For staging or production, `MEDIA_STORAGE_DIRECTORY` must point at a persistent
mounted volume before enabling editor integration. Replacing the local storage
adapter with object storage remains the intended production-scale option.
