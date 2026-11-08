# Case Management System

A backend application built using **Java 21 and Spring Boot** to manage cases, handle user access, process asynchronous events using Apache Kafka, and generate operational reports.

The application follows a modular architecture and integrates MySQL, Redis, Kafka, and Zipkin to demonstrate real-world backend development concepts.

## 🚀 Key Features

### 1. Case Management
- Create, retrieve, update, and soft-delete cases.
- Support for case statuses: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`.
- Support for priorities: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
- Add and retrieve comments for cases.
- Pagination, sorting, and filtering.
- Request validation and standardized error responses.
- Idempotent case creation using idempotency keys.

### 2. Authentication and Authorization
- JWT-based authentication.
- Role-Based Access Control (RBAC).
- Password encryption using BCrypt.
- Method-level authorization using Spring Security.
- Three user roles:
  - **ADMIN** – Full access to case management operations.
  - **AGENT** – Create, view, and update cases, and add comments.
  - **VIEWER** – Read-only access.

### 3. Rate Limiting
- Distributed rate limiting using Redis.
- User-based rate limiting for authenticated requests.
- IP-based rate limiting for unauthenticated requests.
- HTTP 429 responses when rate limits are exceeded.
- Rate-limit response headers.

### 4. Event Processing with Apache Kafka
- Asynchronous case event publishing.
- Event types:
  - `CASE_CREATED`
  - `CASE_UPDATED`
  - `CASE_STATUS_CHANGED`
- Transactional Outbox Pattern to reliably publish database events.
- Kafka consumers with manual offset acknowledgment.
- Configurable retry mechanism with exponential backoff.
- Dead Letter Queue (DLQ) for failed messages.
- Idempotent event processing using unique event IDs.
- Automatic case alert creation for critical-priority cases and cases resolved through status changes.

### 5. Reporting and Analytics
- Case summary report with status and priority counts.
- Agent performance report.
- Case alert report with filtering and pagination.
- Database-level aggregation using SQL queries and CTEs.

### 6. Observability
- Spring Boot Actuator for application health and metrics.
- Custom business metrics using Micrometer.
- Structured application logging.
- Correlation IDs for request tracking.
- Distributed tracing using Micrometer Tracing and Zipkin.

## 🛠️ Technology Stack

| Category | Technologies |
|---|---|
| Language | Java 21 |
| Backend | Spring Boot 4 |
| ORM | Spring Data JPA, Hibernate |
| Database | MySQL 8 |
| Security | Spring Security, JWT |
| Caching / Rate Limiting | Redis 7 |
| Messaging | Apache Kafka |
| API Documentation | Springdoc OpenAPI / Swagger |
| Monitoring | Spring Boot Actuator, Micrometer |
| Distributed Tracing | Zipkin |
| Build Tool | Maven |
| Containerization | Docker, Docker Compose |

## 🏗️ Architecture Overview

The application follows a modular monolithic architecture, with clearly separated business capabilities.

```text
                   Client
                     |
                     v
              Spring Security
                JWT Filter
                     |
                     v
               Rate Limiting
                  (Redis)
                     |
                     v
             REST API Controllers
                     |
                     v
              Service Layer
                     |
          +----------+----------+
          |          |          |
          v          v          v
        MySQL     Reporting   Outbox
                              Events
                                 |
                                 v
                         Outbox Publisher
                                 |
                                 v
                            Kafka Topic
                                 |
                                 v
                          Kafka Consumer
                                 |
                                 v
                           Alert Service
                                 |
                                 v
                             MySQL

          Observability: Actuator + Micrometer + Zipkin
```

## 📌 API Endpoints

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/login` | Authenticate and generate JWT |

### Case Management

| Method | Endpoint | Description |
|---|---|---|
| POST | `/cases` | Create a case |
| GET | `/cases` | Retrieve cases with filters and pagination |
| GET | `/cases/{id}` | Retrieve a case by ID |
| PATCH | `/cases/{id}` | Update case details |
| DELETE | `/cases/{id}` | Soft-delete a case |
| POST | `/cases/{id}/comments` | Add a comment |
| GET | `/cases/{id}/comments` | Retrieve case comments |

### Reporting

| Method | Endpoint | Description |
|---|---|---|
| GET | `/reports/cases/summary` | Case summary report |
| GET | `/reports/agents/performance` | Agent performance report |
| GET | `/reports/alerts` | Retrieve case alerts |

### Monitoring

| Method | Endpoint | Description |
|---|---|---|
| GET | `/actuator/health` | Application health |
| GET | `/actuator/metrics` | Application metrics |

For complete API details, refer to the Swagger UI after starting the application.

## ⚙️ Running the Application

### Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop
- Git

### 1. Clone the repository

```bash
git clone https://github.com/harsha-korrapati/case-management.git
cd case-management
```

### 2. Configure environment variables

Create a `.env` file in the project root.

Configure the required values:

```env
MYSQL_ROOT_PASSWORD=your_mysql_password
JWT_SECRET=your_base64_encoded_jwt_secret
```

Use a securely generated Base64-encoded JWT secret. Never commit `.env` or real credentials to GitHub.

### 3. Start the application using Docker Compose

```bash
docker compose up -d --build
```

This starts the application and its supporting infrastructure, including MySQL, Redis, Kafka, and Zipkin, as configured in Docker Compose.

### 4. Verify application health

```bash
docker compose ps
```

Check the health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

### 5. Access the application

| Service | URL |
|---|---|
| Application | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Actuator Health | http://localhost:8080/actuator/health |
| Zipkin UI | http://localhost:9411 |
