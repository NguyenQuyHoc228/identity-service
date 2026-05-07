# 🔐 Identity Service

REST API service for **Authentication** and **Authorization** using JWT + RBAC.

## Tech Stack
- Java 21, Spring Boot 3.2.5
- Spring Security + OAuth2 Resource Server
- MySQL 8.0 + Spring Data JPA + Hibernate
- MapStruct, Lombok
- JUnit 5 + Mockito (16 test cases)

## Features
- ✅ JWT Authentication (Login, Logout, Refresh Token, Introspect)
- ✅ Role-Based Access Control (RBAC): User → Role → Permission
- ✅ BCrypt password encoding
- ✅ Token blacklisting on logout
- ✅ Scheduled cleanup of expired tokens
- ✅ Custom validation annotation (@DobConstraint)
- ✅ Global exception handling with consistent ApiResponse format
- ✅ Unit Tests (16 test cases)

## How to Run

### 1. Tạo database
```sql
CREATE DATABASE identity_service
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

### 2. Cấu hình application.yaml
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/identity_service
    username: your_username
    password: your_password
```

### 3. Chạy project
```bash
mvn spring-boot:run
```
App chạy tại: `http://localhost:8080`

## API Endpoints

| Method | URL | Description | Auth |
|--------|-----|-------------|------|
| POST | `/identity/auth/token` | Login, get JWT | Public |
| POST | `/identity/auth/introspect` | Check token | Public |
| POST | `/identity/auth/logout` | Logout | Public |
| POST | `/identity/auth/refresh` | Refresh token | Public |
| POST | `/identity/users` | Create user | Public |
| GET | `/identity/users` | Get all users | ADMIN |
| GET | `/identity/users/myInfo` | My info | Authenticated |
| GET | `/identity/users/{id}` | Get user by ID | Owner/ADMIN |
| PUT | `/identity/users/{id}` | Update user | Authenticated |
| DELETE | `/identity/users/{id}` | Delete user | Authenticated |
| POST/GET/DELETE | `/identity/permissions` | CRUD permissions | Authenticated |
| POST/GET/DELETE | `/identity/roles` | CRUD roles | Authenticated |

## Architecture
Controller → Service → Repository → Database
↓
Mapper (MapStruct)
↓
DTO (Request/Response)

## Security Flow
Request → BearerTokenAuthenticationFilter
↓
CustomJwtDecoder (check blacklist)
↓
JwtAuthenticationConverter
↓
@PreAuthorize / @PostAuthorize

## Default Admin Account
Username: admin
Password: admin
> ⚠️ Change password immediately in production!
