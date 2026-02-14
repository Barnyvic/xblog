# xblog

REST API for a simple blog: auth (JWT), user profile, and posts.

## Stack

- **Java 25**, **Spring Boot 4**
- **SQLite** (file DB), **Redis** (optional; cache uses in-memory by default)
- **Spring Security** + **JWT** (RS256)
- **Spring Data JPA**, **SpringDoc OpenAPI** (Swagger)

## Structure

- **`/api/auth`** – signup, login (no auth required)
- **`/api/users/me`** – current user profile (JWT required)
- **`/api/posts`** – CRUD for blog posts (create/update/delete require JWT)

Controllers delegate to services; services use repositories and shared mappers. Exceptions are handled by `GlobalExceptionHandler`; API responses use a common `ApiResponse<T>` wrapper.

## Run

```bash
./mvnw spring-boot:run
```

App runs at **http://localhost:8080**. Swagger UI: **http://localhost:8080/swagger-ui.html**.

## Config

- **DB:** SQLite file `./xblog.db` (created automatically).
- **Redis:** `spring.data.redis.host` / `spring.data.redis.port` (optional; cache works without it).
- **JWT:** Set `JWT_PRIVATE_KEY` and `JWT_PUBLIC_KEY` (base64) for production; otherwise an in-memory key is used (tokens invalid after restart). `JWT_VALIDITY_MS` (default 24h) controls token expiry.

Use a `.env` or env vars; no secrets in `application.properties`.

## Tests

```bash
./mvnw test
```

Unit tests (e.g. `UserServiceTest`) and controller tests (e.g. `AuthControllerTest`) use Mockito and MockMvc. See **`docs/TESTING.md`** for a short walkthrough.
