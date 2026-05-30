# IncidentHub — Technology Stack & Design Principles Documentation

## Table of Contents
1. [Backend Technologies](#backend-technologies)
2. [Frontend Technologies](#frontend-technologies)
3. [Database & Caching](#database--caching)
4. [Security](#security)
5. [Communication & Real-Time](#communication--real-time)
6. [DevOps & CI/CD](#devops--cicd)
7. [Design Principles & Patterns](#design-principles--patterns)
8. [Custom Algorithms & Data Structures](#custom-algorithms--data-structures)
9. [Architecture Decisions](#architecture-decisions)

---

## Backend Technologies

### 1. Java 17 (LTS)
**What:** Long-Term Support version of Java with modern language features like records, sealed classes, pattern matching, and enhanced switch expressions.

**Why chosen:** Java 17 is the industry-standard LTS release for enterprise applications. It offers a balance between modern features and stability, making it the most widely supported version in production environments.

**Best Practice:** Always use LTS versions for production applications to ensure long-term security patches and community support. Non-LTS versions lose support within 6 months.

---

### 2. Spring Boot 3.2.5
**What:** An opinionated framework built on top of the Spring Framework that simplifies the creation of production-ready applications with embedded servers, auto-configuration, and convention-over-configuration.

**What it does in this project:** Provides the entire backend application framework — dependency injection, auto-configuration of JPA, Security, WebSocket, Mail, Cache, and embedded Tomcat server.

**Why chosen:** Spring Boot eliminates boilerplate configuration, provides production-ready features (health checks, metrics, externalized config), and has the largest enterprise Java ecosystem.

**Best Practice:** Spring Boot follows the "convention over configuration" principle — it works out-of-the-box with sensible defaults but allows full customization when needed.

---

### 3. Spring Data JPA (Hibernate 6.4)
**What:** An abstraction layer over JPA (Java Persistence API) that eliminates boilerplate data access code. Hibernate is the underlying ORM (Object-Relational Mapping) implementation.

**What it does in this project:** Maps Java entities (Incident, User, Team, Attachment, NotificationLog, etc.) to database tables. Provides repository interfaces with auto-generated queries, pagination, sorting, and custom JPQL/HQL queries.

**Why chosen:** Eliminates 80%+ of data access code. Method-name-based query derivation (`findByStatusAndSeverity`) makes code readable and maintainable without writing SQL.

**Best Practice:** Use the Repository pattern to separate business logic from data access. Leverage Spring Data's query derivation for simple queries, and `@Query` annotation for complex ones.

**Design Principle:** *Repository Pattern* — Encapsulates data access logic, making the application database-agnostic and testable.

---

### 4. Spring Security 6
**What:** A powerful and highly customizable authentication and access-control framework for Spring applications.

**What it does in this project:**
- JWT-based stateless authentication (no server-side sessions)
- Role-based access control (ADMIN, MANAGER, ENGINEER)
- Method-level security with `@PreAuthorize`
- CORS configuration for frontend communication
- Password encryption with BCrypt

**Why chosen:** Industry-standard security framework that integrates seamlessly with Spring Boot. Supports multiple authentication mechanisms and provides defense against CSRF, session fixation, clickjacking, etc.

**Best Practice:** Stateless JWT authentication is ideal for microservices and horizontal scaling — no session replication needed across instances.

**Design Principle:** *Defense in Depth* — Multiple layers of security (URL-level + method-level + role-based).

---

### 5. Spring WebSocket (STOMP over SockJS)
**What:** Full-duplex communication protocol enabling real-time bidirectional data flow between server and client. STOMP provides message-level protocol, SockJS provides fallback for browsers without WebSocket support.

**What it does in this project:** Pushes real-time notifications to connected clients when incidents are created, updated, escalated, or SLA is breached — without polling.

**Why chosen:** Provides instant UI updates without periodic HTTP polling, reducing latency from seconds to milliseconds and reducing server load.

**Best Practice:** Use STOMP for structured messaging (topic-based pub/sub) rather than raw WebSocket for maintainability and broker compatibility.

**Design Principle:** *Event-Driven Architecture* — Components communicate through events rather than direct calls.

---

### 6. Spring Boot Starter Mail + Thymeleaf
**What:** JavaMailSender abstraction for sending emails. Thymeleaf is a server-side template engine for generating dynamic HTML content.

**What it does in this project:**
- Sends HTML email notifications on incident creation, escalation, and resolution
- Thymeleaf renders professional email templates with incident details, severity badges, and action links
- Async execution (`@Async`) ensures email sending doesn't block the main thread
- Notification logging tracks every sent/failed email in the database

**Why chosen:** Spring Mail provides a clean abstraction over JavaMail API. Thymeleaf generates maintainable HTML templates with natural templating (templates are valid HTML even without server rendering).

**Best Practice:** Always send emails asynchronously to avoid blocking request threads. Log all notification attempts for audit trails and debugging.

**Design Principle:** *Asynchronous Processing* — Non-critical operations (email) should never block user-facing requests.

---

### 7. Spring Retry
**What:** Provides declarative retry support for Spring applications using `@Retryable` annotation with configurable backoff policies.

**What it does in this project:** Retries failed external service calls (email sending, database operations) with exponential backoff to handle transient failures gracefully.

**Why chosen:** Transient failures (network glitches, temporary DB unavailability) are common in distributed systems. Retry with backoff prevents cascading failures.

**Best Practice:** Use exponential backoff (not fixed intervals) to avoid thundering herd problems. Set max retry attempts to prevent infinite loops.

**Design Principle:** *Resilience Pattern* — Applications should be fault-tolerant and recover gracefully from transient failures.

---

### 8. Spring Boot Actuator
**What:** Production-ready features for monitoring and managing Spring Boot applications — health checks, metrics, environment info, thread dumps.

**What it does in this project:**
- `/actuator/health` — Load balancer health endpoint (custom `LoadBalancerHealthIndicator`)
- `/actuator/prometheus` — Exposes metrics in Prometheus format
- Custom drain/resume endpoints for graceful deployments

**Why chosen:** Essential for production deployments — load balancers need health checks, operations teams need metrics, and graceful shutdown requires drain support.

**Best Practice:** Expose only necessary actuator endpoints in production. Secure sensitive endpoints behind ADMIN role.

**Design Principle:** *Observability* — Systems must be observable to be operable.

---

### 9. Spring AOP (Aspect-Oriented Programming)
**What:** Programming paradigm that allows cross-cutting concerns (logging, security, rate limiting) to be modularized into reusable aspects.

**What it does in this project:**
- Custom `@RateLimit` annotation with AOP aspect for per-IP/per-user rate limiting
- `@DataSourceRoutingAspect` for read/write database routing
- Cross-cutting concerns separated from business logic

**Why chosen:** AOP keeps business code clean by extracting infrastructure concerns (rate limiting, routing, auditing) into separate aspects.

**Best Practice:** Use AOP for cross-cutting concerns only — not for business logic. Keep aspects simple and focused.

**Design Principle:** *Separation of Concerns* — Each module handles one responsibility. Cross-cutting concerns belong in aspects, not scattered across business code.

---

### 10. Micrometer + Prometheus
**What:** Micrometer is a metrics facade (like SLF4J for metrics). Prometheus is a time-series database for metrics storage and alerting.

**What it does in this project:**
- Custom business metrics: `incidents_created_total`, `incidents_resolved_total`, `incident_resolution_time_seconds`
- JVM metrics: memory, threads, GC
- HTTP metrics: request rate, latency, error rate
- Exposed at `/actuator/prometheus` for Prometheus scraping

**Why chosen:** Industry standard for application monitoring. Micrometer's vendor-neutral API means you can switch from Prometheus to Datadog/CloudWatch without code changes.

**Best Practice:** Track business metrics (not just infrastructure metrics). Use histograms for latency, counters for events, gauges for current state.

**Design Principle:** *Metrics-Driven Development* — If you can't measure it, you can't improve it.

---

### 11. Springdoc OpenAPI 2.5 (Swagger)
**What:** Automatically generates OpenAPI 3.0 documentation from Spring MVC annotations. Provides Swagger UI for interactive API exploration.

**What it does in this project:** Auto-generates API documentation at `/swagger-ui.html` with all endpoints, request/response schemas, authentication requirements, and try-it-out functionality.

**Why chosen:** Eliminates manual API documentation maintenance. Frontend developers and external consumers can discover and test APIs without reading source code.

**Best Practice:** API documentation should be auto-generated from code to stay in sync. Manual docs always drift from reality.

---

### 12. Lombok
**What:** Java annotation processor that generates boilerplate code (getters, setters, constructors, builders, toString, equals/hashCode) at compile time.

**What it does in this project:** Eliminates hundreds of lines of boilerplate in entity and DTO classes using `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`.

**Why chosen:** Java's verbosity is a known pain point. Lombok reduces noise while maintaining full IDE support (auto-complete, navigation).

**Best Practice:** Use `@RequiredArgsConstructor` for dependency injection (final fields) — cleaner than `@Autowired` and supports immutability.

---

### 13. Maven (Build Tool)
**What:** Project management and build automation tool that manages dependencies, compilation, testing, packaging, and deployment lifecycle.

**What it does in this project:** Manages all Java dependencies, compiles source code, runs tests, packages the application as an executable JAR with embedded Tomcat.

**Why chosen:** Maven's convention-over-configuration approach and central repository make dependency management reliable and reproducible across environments.

**Best Practice:** Use Maven Wrapper (`mvnw`) to ensure all developers use the same Maven version without manual installation.

---

## Frontend Technologies

### 14. React 18 (TypeScript)
**What:** A declarative, component-based JavaScript library for building user interfaces. TypeScript adds static type checking to JavaScript.

**What it does in this project:** Builds the entire single-page application — Dashboard, Incident Management, Analytics, Share Links, real-time notifications. Component-based architecture with hooks for state management.

**Why chosen:** React's virtual DOM provides efficient UI updates (critical for real-time dashboards). TypeScript catches type errors at compile time, reducing runtime bugs by 15-30%.

**Best Practice:** Use functional components with hooks (not class components). TypeScript interfaces for API response types ensure frontend-backend contract safety.

**Design Principle:** *Component-Based Architecture* — UI broken into reusable, self-contained components with clear interfaces.

---

### 15. React Router DOM 6
**What:** Client-side routing library for React applications enabling SPA navigation without full page reloads.

**What it does in this project:** Handles navigation between Dashboard, Incidents, Incident Detail, Analytics, and Share Links pages. Supports dynamic routes (`/incidents/:id`) and protected routes.

**Why chosen:** Standard routing solution for React SPAs. V6 introduces simplified API with hooks (`useParams`, `useNavigate`).

**Best Practice:** Use lazy loading for routes to reduce initial bundle size. Implement route guards for authenticated pages.

---

### 16. Axios
**What:** Promise-based HTTP client for browsers with interceptor support, request/response transformation, and automatic JSON parsing.

**What it does in this project:**
- Centralized API client with base URL configuration
- Request interceptor automatically attaches JWT token to all requests
- Response interceptor handles 401 errors (auto-redirect to login)
- Consistent error handling across all API calls

**Why chosen:** Axios interceptors enable cross-cutting concerns (auth, error handling) in one place rather than duplicating in every API call.

**Best Practice:** Create a single configured Axios instance. Use interceptors for authentication and global error handling. Never scatter token logic across components.

**Design Principle:** *DRY (Don't Repeat Yourself)* — Authentication logic defined once, applied everywhere.

---

### 17. STOMP.js + SockJS
**What:** STOMP.js is a JavaScript client for STOMP protocol messaging. SockJS provides WebSocket emulation with fallback transports (XHR streaming, long-polling).

**What it does in this project:** Connects to the Spring WebSocket broker, subscribes to incident topics, and receives real-time notifications (incident created, updated, escalated, SLA breached) displayed as toast messages.

**Why chosen:** SockJS ensures WebSocket functionality works even behind corporate proxies/firewalls that block native WebSocket connections.

**Best Practice:** Always use SockJS as a fallback layer — never assume native WebSocket support. Implement reconnection logic for dropped connections.

---

## Database & Caching

### 18. PostgreSQL (Production)
**What:** Advanced open-source relational database with ACID compliance, JSONB support, full-text search, and excellent concurrency handling (MVCC).

**What it does in this project:** Production database storing all incident data, user accounts, teams, attachments metadata, notification logs, and URL mappings.

**Why chosen:** PostgreSQL handles complex queries, concurrent writes, and large datasets better than MySQL. Its MVCC architecture means readers never block writers — critical for real-time incident systems.

**Best Practice:** Use connection pooling (HikariCP), proper indexing, and prepared statements. Never expose database directly to the internet.

---

### 19. H2 Database (Development)
**What:** Lightweight, in-memory Java SQL database ideal for development and testing.

**What it does in this project:** Provides instant database for local development with `create-drop` DDL mode — schema recreated on each restart. Web console at `/h2-console` for quick data inspection.

**Why chosen:** Zero-configuration development experience. Developers don't need to install/configure PostgreSQL locally. Same JPA code works with both H2 and PostgreSQL.

**Best Practice:** Use Spring Profiles (`dev`/`prod`) to switch databases. Development should be as frictionless as possible while production uses battle-tested databases.

**Design Principle:** *Environment Parity with Pragmatism* — Keep dev and prod similar, but don't sacrifice developer productivity for absolute parity.

---

### 20. Redis (Caching & Distributed Lock)
**What:** In-memory data structure store used as cache, message broker, and distributed lock manager.

**What it does in this project:**
- **Caching:** Dashboard stats and analytics results cached to avoid expensive DB queries on every request
- **Distributed Lock:** Ensures only one application instance runs the escalation scheduler at a time (prevents duplicate escalations in multi-instance deployments)
- **Rate Limiting:** Token bucket state stored in Redis for distributed rate limiting

**Why chosen:** Redis provides sub-millisecond reads for cached data and atomic operations for distributed coordination — both critical for real-time systems.

**Best Practice:** Set TTL (time-to-live) on all cache entries. Use `@CacheEvict` on write operations to prevent stale data. Implement cache-aside pattern.

**Design Principle:** *Cache-Aside Pattern* — Application checks cache first, falls through to DB on miss, and populates cache on read.

---

### 21. HikariCP (Connection Pooling)
**What:** High-performance JDBC connection pool — the fastest and most reliable Java connection pool available.

**What it does in this project:** Manages database connection lifecycle — pre-creates connections, reuses them across requests, and handles connection validation/eviction.

**Why chosen:** Spring Boot's default connection pool. HikariCP is 30-50x faster than competitors (C3P0, DBCP) due to its bytecode-level optimizations and lock-free design.

**Best Practice:** Configure pool size based on: `connections = (core_count * 2) + effective_spindle_count`. Monitor pool metrics for connection leaks.

---

## Security

### 22. JWT (JSON Web Tokens) — jjwt 0.12.5
**What:** Compact, URL-safe token format for securely transmitting claims between parties. Self-contained (server doesn't need to store session state).

**What it does in this project:**
- Issued on successful login with 24-hour expiration
- Contains: username, role, user ID, issuance/expiry timestamps
- Validated on every request by `JwtAuthenticationFilter`
- Stored in browser's `sessionStorage` (cleared on tab close)

**Why chosen:** Stateless authentication enables horizontal scaling — any server instance can validate the token without shared session storage.

**Best Practice:** Short expiration times (24h) + refresh tokens. Never store sensitive data in JWT payload (it's base64-encoded, not encrypted). Use `sessionStorage` over `localStorage` for XSS mitigation.

**Design Principle:** *Stateless Authentication* — Server holds no session state, enabling infinite horizontal scalability.

---

### 23. BCrypt Password Hashing
**What:** Adaptive hash function specifically designed for password storage with built-in salt and configurable work factor.

**What it does in this project:** Hashes user passwords before storage. Each hash includes a unique salt, making rainbow table attacks impossible. Work factor makes brute-force computationally expensive.

**Why chosen:** BCrypt is the gold standard for password hashing. Unlike SHA/MD5, it's intentionally slow (configurable), making GPU-based cracking impractical.

**Best Practice:** Never store plaintext passwords. BCrypt's adaptive cost factor should be increased as hardware improves (currently cost=10 is standard).

---

### 24. Role-Based Access Control (RBAC)
**What:** Access control model where permissions are assigned to roles, and roles are assigned to users.

**What it does in this project:**
- **ADMIN:** Full access (delete incidents, view actuator, manage users, view notification logs)
- **MANAGER:** Moderate access (delete incidents, view analytics, view notification logs)
- **ENGINEER:** Operational access (create/update/acknowledge incidents)

**Why chosen:** RBAC is simpler to manage than attribute-based (ABAC) and sufficient for team-based incident management where clear role boundaries exist.

**Best Practice:** Implement at both URL level (SecurityConfig) and method level (`@PreAuthorize`) for defense-in-depth.

---

## Communication & Real-Time

### 25. REST API Architecture
**What:** Architectural style for networked applications using HTTP methods (GET, POST, PUT, DELETE) on resource-oriented URLs.

**What it does in this project:** All frontend-backend communication follows REST conventions:
- `GET /api/incidents` — List resources
- `POST /api/incidents` — Create resource
- `PUT /api/incidents/{id}` — Update resource
- `DELETE /api/incidents/{id}` — Delete resource

**Why chosen:** REST is universally understood, stateless, cacheable, and works with any HTTP client. Combined with OpenAPI docs, any developer can consume the API.

**Best Practice:** Use proper HTTP status codes (201 Created, 404 Not Found, 403 Forbidden). Use nouns for URLs, HTTP methods for actions.

**Design Principle:** *Uniform Interface* — Consistent resource naming and HTTP semantics reduce cognitive load for API consumers.

---

### 26. Custom Sliding Window Rate Limiter (AOP)
**What:** Traffic control mechanism that limits API requests per user/IP within a sliding time window. Uses a sliding window log algorithm with a `ConcurrentLinkedDeque` of request timestamps.

**Algorithm — Sliding Window Log:**
```
1. For each incoming request, identify the key (IP / userId / GLOBAL)
2. Get or create a SlidingWindow object for the key (ConcurrentHashMap lookup)
3. Remove all timestamps older than (now - windowMillis) from the front of the deque
4. If remaining count < maxRequests → allow request, add timestamp to deque
5. Else → reject with 429 Too Many Requests + Retry-After header
```

**Data Structures Used:**
- `ConcurrentHashMap<String, SlidingWindowRateLimiter>` — one limiter per endpoint (annotation-driven)
- `ConcurrentHashMap<String, SlidingWindow>` — one window per client key
- `ConcurrentLinkedDeque<Long>` — timestamp log per client (O(1) head removal, O(1) tail insert)
- `AtomicInteger` — fast count without full deque traversal

**Thread Safety:** `synchronized` per-window block ensures correctness without global lock contention.

**Why Sliding Window over Fixed Window:** Fixed window has the boundary problem — a burst at the edge of two windows allows 2x the limit. Sliding window counts across the entire moving window, providing accurate enforcement.

**Why Sliding Window over Token Bucket:** Token bucket allows uncapped bursts if tokens have accumulated. Sliding window enforces a hard cap within any window period — more appropriate for API abuse prevention.

**Configuration (per endpoint):**
```java
@RateLimit(maxRequests = 10, windowSeconds = 60, keyType = RateLimit.KeyType.USER)
```
- Login: 10 requests/60s per IP (brute-force protection)
- Create Incident: 10 requests/60s per User
- Read Incidents: 100 requests/60s per IP
- Dashboard Stats: 60 requests/60s per IP

**Cleanup:** Background scheduled task (`RateLimiterCleanupTask`) evicts stale entries where `lastAccessTime + 2*window < now` to prevent memory leaks.

**What it does in this project:** Custom `@RateLimit` annotation intercepted by `RateLimitAspect` (AOP Around advice). Automatically resolves client key, enforces limits, and sets HTTP headers (`X-RateLimit-Remaining`, `X-RateLimit-Reset`).

**Why custom implementation:** Production rate limiters (Redis-based, Spring Cloud Gateway) add infrastructure dependencies. This in-memory implementation demonstrates the algorithm knowledge while being production-functional for single-instance deployments.

**Design Principle:** *API Gateway Pattern* — Protect backend services from abuse at the edge.

**Complexity:** O(k) per request where k = number of expired timestamps to evict (amortized O(1) in practice).

---

## DevOps & CI/CD

### 27. GitHub Actions
**What:** CI/CD platform integrated into GitHub that automates build, test, and deployment pipelines triggered by git events.

**What it does in this project:**
- **Build Job:** Compiles Java 17 code, runs unit + integration tests with Redis service container, packages JAR
- **Frontend Job:** Installs Node 18 dependencies, builds React production bundle
- **Artifacts:** Uploads JAR and frontend build for deployment

**Why chosen:** Native GitHub integration means zero additional infrastructure. Free for public repos with generous limits for private repos.

**Best Practice:** Run tests on every push/PR. Cache dependencies (Maven, npm) to speed up builds. Use service containers for integration tests.

**Design Principle:** *Continuous Integration* — Every code change is automatically validated, preventing integration issues from accumulating.

---

### 28. Spring Profiles (dev/prod)
**What:** Environment-specific configuration mechanism that activates different property sets based on the active profile.

**What it does in this project:**
- **dev:** H2 in-memory DB, Redis excluded, mail disabled, CORS open, DDL auto-create
- **prod:** PostgreSQL, Redis enabled, mail enabled, strict CORS, DDL validate-only

**Why chosen:** Same codebase runs in all environments with different configurations — no code changes needed for deployment.

**Best Practice:** Never commit production credentials. Use environment variables (`${DB_USERNAME}`) for sensitive values. Default to the safest configuration.

**Design Principle:** *12-Factor App (Config)* — Store config in the environment, not in code.

---

## Design Principles & Patterns

### 29. Layered Architecture
```
Controller Layer  →  Service Layer  →  Repository Layer  →  Database
   (HTTP)              (Business)         (Data Access)
```
**Why:** Clear separation of concerns. Each layer has one responsibility. Changes in one layer don't cascade to others.

---

### 30. DTO Pattern (Data Transfer Object)
**What:** Separate objects for API request/response vs. internal entity representation.

**What it does:** `IncidentDto.CreateRequest` (input) and `IncidentDto.Response` (output) are different from `Incident` entity. This decouples API contract from database schema.

**Why Best Practice:** Prevents accidental data exposure (passwords, internal IDs). Allows API and database to evolve independently.

---

### 31. Builder Pattern
**What:** Creational pattern for constructing complex objects step-by-step with a fluent API.

**What it does:** All entities use `@Builder` (Lombok) for clean, readable object construction:
```java
Incident.builder()
    .title("Server Down")
    .severity(Severity.CRITICAL)
    .reporter(user)
    .build();
```

**Why Best Practice:** Eliminates telescoping constructors. Makes code self-documenting. Immutable objects possible.

---

### 32. Observer Pattern (Event-Driven)
**What:** Objects (observers) subscribe to events and react when events are published, without tight coupling between publisher and subscriber.

**What it does:** WebSocket notifications follow pub/sub — when an incident is created, ALL connected clients receive updates without the service knowing who's listening.

**Why Best Practice:** Decouples components. Adding a new notification channel (Slack, PagerDuty) doesn't modify existing code.

---

### 33. Strategy Pattern (Database Routing)
**What:** Defines a family of algorithms (read replica, write master) and makes them interchangeable at runtime.

**What it does:** `RoutingDataSource` + `DataSourceRoutingAspect` routes read queries to replica and write queries to master based on transaction type.

**Why Best Practice:** Horizontal read scaling without application code changes. New read replicas are automatically utilized.

---

### 34. Scheduler Pattern (Batch Processing)
**What:** Time-triggered background jobs that process data in batches without user interaction.

**What it does:** `EscalationScheduler` runs every 60 seconds, finds SLA-breached incidents, and escalates them in parallel using `CompletableFuture` with a thread pool.

**Why Best Practice:** Automated SLA enforcement without manual intervention. Parallel processing with configurable thread pool for throughput control.

---

### 35. Distributed Lock Pattern
**What:** Coordination mechanism ensuring only one process/instance executes a critical section in a distributed system.

**What it does:** Prevents duplicate escalation when multiple application instances are running. Only one instance acquires the Redis lock and runs the scheduler.

**Why Best Practice:** Essential for correctness in horizontally-scaled systems. Without it, N instances would create N duplicate escalations.

---

## Architecture Decisions

### Custom Algorithms & Data Structures

#### A. Twitter Snowflake ID Generator
**What:** A distributed unique ID generation algorithm originally designed by Twitter for generating 64-bit unique IDs across multiple data centers without coordination.

**64-bit ID Structure:**
```
| 1 bit (unused/sign) | 41 bits (timestamp) | 10 bits (node ID) | 12 bits (sequence) |
|        0            |    ms since epoch    |   machine/DC ID   |   per-ms counter   |
```

**Bit Allocation:**
- **1 bit** — Sign bit (always 0 for positive IDs)
- **41 bits** — Millisecond timestamp since custom epoch (2024-01-01 UTC) → supports ~69 years
- **10 bits** — Node/Machine ID → supports 1,024 unique nodes (datacenter + worker)
- **12 bits** — Sequence counter → 4,096 unique IDs per millisecond per node

**Capacity:** ~4 million unique IDs/second/node. Across 1,024 nodes: ~4 billion IDs/second globally.

**Algorithm:**
```java
1. Get current timestamp in milliseconds
2. If timestamp == lastTimestamp → increment sequence
     If sequence overflows (> 4095) → spin-wait until next millisecond
3. If timestamp > lastTimestamp → reset sequence to 0
4. If timestamp < lastTimestamp → throw ClockMovedBackwards exception
5. Return: ((timestamp - EPOCH) << 22) | (nodeId << 12) | sequence
```

**Why Snowflake over UUID:**
| Property | UUID v4 | Snowflake |
|----------|---------|-----------|
| Size | 128 bits (36 chars) | 64 bits (Long) |
| Sortable | No (random) | Yes (time-ordered) |
| DB Index | Poor (random inserts) | Excellent (sequential) |
| Information | None | Embeds timestamp, node, sequence |
| Performance | Random I/O on B-tree | Sequential I/O (append-mostly) |

**Why Snowflake over Auto-Increment:**
- Auto-increment requires DB coordination (single point of failure)
- Exposes record count to clients (security concern)
- Cannot be generated client-side or in distributed systems
- Snowflake IDs are globally unique without any coordination

**What it does in this project:**
- Primary key generator for `Incident`, `User`, `Team`, `OnCallSchedule` entities
- Implements Hibernate's `IdentifierGenerator` interface for seamless JPA integration
- Custom epoch (2024-01-01) maximizes the 41-bit timestamp range
- Singleton pattern ensures sequence continuity within a JVM
- Utility methods to extract timestamp/nodeId/sequence from any ID (debugging)

**Clock Backward Protection:** If system clock moves backward (NTP sync, VM migration), the generator throws `IllegalStateException` rather than producing duplicate IDs.

**Usage:**
```java
@Id
@GeneratedValue(generator = "snowflake")
@GenericGenerator(name = "snowflake", type = SnowflakeIdGenerator.class)
private Long id;
```

**Design Principle:** *Decentralized ID Generation* — No single point of failure, no coordination overhead, horizontally scalable.

---

#### B. Base62 URL Short Code Generation
**What:** A URL-safe encoding scheme using 62 characters (A-Z, a-z, 0-9) to generate compact, unique short codes for URL shortening.

**Character Set:**
```
ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789
```
62 characters × 7-character code = 62^7 = **3.52 trillion** unique combinations.

**Algorithm:**
```java
1. Generate 7 random characters using SecureRandom (cryptographically secure)
2. Each character picked from the 62-char alphabet uniformly
3. Check DB for collision (existsByShortCode)
4. If collision → regenerate (loop until unique)
5. Return unique code
```

**Why Base62 over Base64:**
- Base64 includes `+`, `/`, `=` — not URL-safe without encoding
- Base62 is inherently URL-safe — no special characters needed
- Shorter URLs are more shareable (Slack, Teams, SMS)

**Why 7 Characters:**
- 62^6 = 56 billion (sufficient for most apps)
- 62^7 = 3.52 trillion (virtually collision-free even at scale)
- 62^8 = 218 trillion (overkill for this use case)
- 7 chars strikes the balance: short enough to share, long enough to be unique

**Collision Probability (Birthday Problem):**
- With 1 million URLs: collision probability ≈ 1 in 3.5 million (negligible)
- With 1 billion URLs: collision probability ≈ 0.014% per generation
- Retry loop handles the rare collision case

**Security:** Uses `java.security.SecureRandom` (not `Math.random()`) — cryptographically secure PRNG prevents short code prediction/enumeration.

**What it does in this project:**
- Generates compact shareable links for incident URLs
- Click tracking per short URL
- Optional expiry (TTL in days)
- Per-user URL management (list, stats, delete)

**Design Principle:** *Information Hiding* — Short codes are opaque; original URLs are not derivable from the code.

---

#### C. Redis Distributed Lock (Mutex)
**What:** A coordination primitive using Redis `SETNX` (SET if Not eXists) with TTL to implement mutual exclusion across distributed application instances.

**Algorithm:**
```
1. SETNX lock_key "instance_id" EX ttl_seconds
   - If key doesn't exist → set it with TTL → lock acquired
   - If key exists → lock held by another instance → return false
2. On unlock: DEL lock_key (only if value matches instance_id)
3. TTL acts as deadlock prevention — lock auto-expires if holder crashes
```

**Why Redis Lock over DB Lock:**
- Redis operations are atomic and sub-millisecond
- DB row locks hold connections and can cause deadlocks
- Redis TTL provides automatic deadlock recovery
- Works across multiple database instances

**What it does in this project:**
- `EscalationScheduler` acquires lock `"escalation_lock"` with 55s TTL (cron runs every 60s)
- Only one instance across the cluster runs SLA breach detection
- If holder crashes, lock auto-releases in 55s and another instance takes over
- Graceful fallback: if Redis is unavailable, lock is granted (single-instance mode)

**Design Principle:** *Leader Election* — In a distributed system, exactly one node performs the scheduled work at any given time.

---

#### D. Concurrent Data Structures Used

| Data Structure | Location | Purpose |
|---------------|----------|---------|
| `ConcurrentHashMap` | Rate Limiter | Lock-free per-key window storage |
| `ConcurrentLinkedDeque` | Sliding Window | O(1) timestamp log with head eviction |
| `AtomicInteger` | Sliding Window | Lock-free request count |
| `CompletableFuture` | Escalation Scheduler | Parallel incident processing |
| `volatile` | Snowflake Generator | Visibility of lastTimestamp across threads |
| `synchronized` | Snowflake nextId() | Atomicity of timestamp+sequence combo |

---

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Authentication | JWT (stateless) | Horizontal scaling without session replication |
| Real-time updates | WebSocket | Sub-100ms latency vs. polling's 5-60s delay |
| Caching | Redis | Distributed cache shared across instances |
| Email sending | Async (@Async) | Non-blocking request handling |
| Database routing | AOP + Strategy | Transparent read/write splitting |
| Rate limiting | Custom AOP | Per-endpoint granularity with annotation config |
| Scheduling | Spring @Scheduled + Lock | Reliable, distributed-safe background jobs |
| API versioning | URL-based (/v2) | Backward compatibility for existing clients |
| File storage | Local filesystem | Simple, works without cloud dependencies |
| Frontend state | sessionStorage | Cleared on tab close for security |
| Monitoring | Micrometer + Prometheus | Vendor-neutral metrics, industry standard |
| Documentation | Springdoc OpenAPI | Auto-generated, always in sync with code |

---

## Summary

This project demonstrates a **production-ready, enterprise-grade** incident management platform covering:

- **14 Spring Boot modules** working together cohesively
- **7 design patterns** applied in appropriate contexts
- **4 custom algorithms** (Snowflake ID, Sliding Window Rate Limiter, Base62 Encoding, Distributed Lock)
- **6 concurrent data structures** (ConcurrentHashMap, ConcurrentLinkedDeque, AtomicInteger, CompletableFuture, volatile, synchronized)
- **Full-stack** implementation with type-safe frontend-backend integration
- **Real-time** capabilities with WebSocket
- **Security** at multiple layers (network, application, data)
- **Observability** with metrics, health checks, and audit logs
- **Scalability** patterns (stateless auth, caching, distributed locks, read replicas)
- **CI/CD** with automated testing and artifact generation
- **Developer Experience** with profiles, H2 console, Swagger UI, and hot-reload
