# IncidentHub - Real-Time Incident Management Platform

A full-stack enterprise incident management system built with **Spring Boot** and **React**, featuring real-time notifications, analytics, SLA tracking, and more.

## Screenshots

### Login
![Login](frontend/public/login.png)

### Dashboard
![Dashboard](frontend/public/Dashboard.png)

### Analytics
![Analytics](frontend/public/analytics.png)

## Tech Stack

### Backend
- **Java 17** + **Spring Boot 3.2.5**
- **Spring Security** with JWT Authentication
- **Spring Data JPA** with Hibernate
- **H2** (dev) / **PostgreSQL** (prod)
- **Redis** for caching & distributed locks
- **Spring Retry** with exponential backoff
- **WebSocket (STOMP)** for real-time updates
- **Micrometer + Prometheus** for custom metrics
- **SpringDoc OpenAPI** (Swagger UI)

### Frontend
- **React 18** + **TypeScript**
- **React Router v6**
- **Axios** with interceptors
- **STOMP.js** + **SockJS** for WebSocket
- Dark/Light theme support

## Features

- **Incident CRUD** — Create, update, assign, resolve incidents
- **Real-Time Dashboard** — Live stats with WebSocket push
- **SLA Monitoring** — Auto-escalation on SLA breach
- **Role-Based Access** — ADMIN, MANAGER, ENGINEER roles
- **Analytics (MTTR/MTTA)** — Mean time to resolve/acknowledge, SLA compliance
- **Audit Logging** — Async audit trail for all actions
- **API Versioning** — v1 and v2 REST APIs
- **URL Shortener** — Built-in URL shortener service with click tracking
- **Redis Caching** — Dashboard stats, analytics cached with TTL
- **Distributed Lock** — Prevents duplicate scheduler execution
- **Retry with Backoff** — Auto-retry on transient failures
- **Rate Limiting** — Per-user API rate limiter
- **Custom Metrics** — Prometheus-compatible business metrics
- **Dark/Light Theme** — Toggle between themes
- **Load Balancer Ready** — Health checks + read/write splitting

## API Endpoints

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login & get JWT token |
| POST | `/api/auth/register` | Register new user |

### Incidents
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/incidents` | List incidents (paginated) |
| POST | `/api/incidents` | Create incident |
| GET | `/api/incidents/{id}` | Get incident detail |
| PATCH | `/api/incidents/{id}/status` | Update status |
| PATCH | `/api/incidents/{id}/acknowledge` | Acknowledge |
| DELETE | `/api/incidents/{id}` | Delete (ADMIN/MANAGER) |
| GET | `/api/incidents/dashboard/stats` | Dashboard statistics |

### Analytics & Audit (v2)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v2/analytics?days=30` | MTTR, MTTA, SLA compliance |
| GET | `/api/v2/audit` | Audit logs (ADMIN) |
| GET | `/api/v2/audit/entity/{type}/{id}` | Audit by entity |

### URL Shortener
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/urls/shorten` | Shorten a URL |
| GET | `/s/{shortCode}` | Redirect to original |
| GET | `/api/urls` | List your URLs |
| GET | `/api/urls/{code}/stats` | Click stats |
| DELETE | `/api/urls/{code}` | Delete short URL |

## Getting Started

### Prerequisites
- Java 17+
- Node.js 16+
- Maven 3.8+

### Backend
```bash
# Run with dev profile (H2 in-memory DB)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
Backend starts at `http://localhost:8080`

### Frontend
```bash
cd frontend
npm install
npm start
```
Frontend starts at `http://localhost:3000`

### Default Credentials
| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| manager1 | password | MANAGER |
| engineer1 | password | ENGINEER |

## Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│   React UI  │────▶│  Spring Boot API │────▶│  Database   │
│  (Port 3000)│◀────│   (Port 8080)    │◀────│  H2 / PG    │
└─────────────┘     └──────────────────┘     └─────────────┘
       │                     │
       │ WebSocket           │ Cache
       ▼                     ▼
┌─────────────┐     ┌──────────────────┐
│  STOMP/WS   │     │     Redis        │
│  Real-time  │     │  (Optional Dev)  │
└─────────────┘     └──────────────────┘
```

## Testing

```bash
# Unit tests
./mvnw test -Dtest=IncidentServiceTest

# Integration tests
./mvnw test -Dtest=IncidentIntegrationTest

# All tests
./mvnw test
```

## Monitoring

- **Health Check**: `GET /actuator/health`
- **Prometheus Metrics**: `GET /actuator/prometheus`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
