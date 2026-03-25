# Smart Campus — Sensor & Room Management API
### 5COSC022W Client-Server Architectures | University of Westminster

A RESTful API built with **JAX-RS (Jersey 2.41)** and an embedded **Grizzly HTTP server**, managing university campus rooms and IoT sensors. All data is stored in-memory using `ConcurrentHashMap` and `ArrayList`. No database or Spring Boot is used.

---

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── Main.java                          ← Starts embedded Grizzly server on port 8080
    ├── SmartCampusApplication.java        ← @ApplicationPath("/api/v1")
    ├── store/DataStore.java               ← Thread-safe ConcurrentHashMap singleton
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── resource/
    │   ├── DiscoveryResource.java         ← GET /api/v1
    │   ├── RoomResource.java              ← GET/POST/DELETE /api/v1/rooms
    │   ├── SensorResource.java            ← GET/POST /api/v1/sensors + sub-resource locator
    │   └── SensorReadingResource.java     ← GET/POST /api/v1/sensors/{id}/readings
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   ├── SensorUnavailableException.java
    │   └── ExceptionMappers.java          ← 409, 422, 403, 404, 500 mappers
    └── filter/
        └── ApiLoggingFilter.java          ← Request/response logging filter
```

---

## API Design Overview

The API follows REST principles with a versioned base path `/api/v1`. Resources are organised around the physical campus structure:

| Resource | Path | Description |
|---|---|---|
| Discovery | `GET /api/v1` | API metadata and HATEOAS links |
| Rooms | `/api/v1/rooms` | Create, list, retrieve, delete campus rooms |
| Sensors | `/api/v1/sensors` | Register, list, filter sensors by type |
| Readings | `/api/v1/sensors/{id}/readings` | Log and retrieve sensor history |

All responses are `application/json`. All errors return structured JSON bodies — no raw stack traces are ever exposed.

---

## How to Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Build

```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
mvn clean package
```

This produces a fat/uber JAR at `target/smart-campus-api-1.0.0.jar`.

### Run

```bash
java -jar target/smart-campus-api-1.0.0.jar
```

The server starts at **http://localhost:8080/api/v1**

You should see:
```
Smart Campus API started successfully!
Base URL : http://localhost:8080/api/v1
```

---

## Sample curl Commands

### 1. Discovery — GET /api/v1
```bash
curl -X GET http://localhost:8080/api/v1 \
  -H "Accept: application/json"
```
Expected: `200 OK` with API version, contact info, and resource links.

---

### 2. Create a Room — POST /api/v1/rooms
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-101","name":"Main Hall","capacity":200}'
```
Expected: `201 Created` with Location header and room object.

---

### 3. Get All Rooms — GET /api/v1/rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms \
  -H "Accept: application/json"
```
Expected: `200 OK` with list of all rooms (includes seeded data).

---

### 4. Register a Sensor with valid roomId — POST /api/v1/sensors
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"LIB-301"}'
```
Expected: `201 Created`.

---

### 5. Register a Sensor with invalid roomId (422 error) — POST /api/v1/sensors
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-000","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-999"}'
```
Expected: `422 Unprocessable Entity` with JSON error body.

---

### 6. Filter Sensors by Type — GET /api/v1/sensors?type=CO2
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2" \
  -H "Accept: application/json"
```
Expected: `200 OK` with only CO2 sensors.

---

### 7. Post a Reading — POST /api/v1/sensors/TEMP-001/readings
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}'
```
Expected: `201 Created`. The parent sensor's `currentValue` is also updated.

---

### 8. Get Reading History — GET /api/v1/sensors/TEMP-001/readings
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Accept: application/json"
```
Expected: `200 OK` with full reading history.

---

### 9. Delete a Room with sensors assigned (409 error)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```
Expected: `409 Conflict` — room still has sensors.

---

### 10. Post reading to MAINTENANCE sensor (403 error)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":5.0}'
```
Expected: `403 Forbidden` — sensor is in MAINTENANCE.

---

## Report: Answers to Coursework Questions

---

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of every resource class for each incoming HTTP request** (request-scoped lifecycle). This is a deliberate design choice for thread safety: because each request gets its own object, there is no shared mutable state within resource instances themselves, so concurrent requests cannot interfere with each other at the instance level.

However, this has a direct consequence for data management. If each resource instance had its own `HashMap`, every request would see an empty, isolated store. To maintain persistent, shared data across all requests, state must live **outside** the resource instances. In this project, the `DataStore` singleton solves this: it is initialised once at application startup and shared across all resource class instances via `DataStore.getInstance()`.

Because the `DataStore` is shared and accessed concurrently by multiple request threads, thread safety is critical. Raw `HashMap` is not thread-safe — concurrent reads and writes can cause `ConcurrentModificationException` or data corruption. This project therefore uses `ConcurrentHashMap`, which provides lock-striped, thread-safe operations without requiring explicit `synchronized` blocks on every method. For the `addReading` method, where a read-then-write sequence must be atomic, an explicit `synchronized` keyword is used to prevent race conditions.

---

### Part 1.2 — HATEOAS (Hypermedia as the Engine of Application State)

HATEOAS is considered a hallmark of mature RESTful design because it makes an API **self-describing and navigable at runtime**. Instead of requiring clients to have prior knowledge of all endpoint URLs — typically found in static documentation — the server embeds links directly in responses, telling the client what actions are available and where to find related resources.

For client developers, this offers significant advantages over static documentation. Documentation goes stale when APIs evolve; embedded links are always current because the server generates them dynamically. Clients can discover new capabilities without re-reading docs or updating hardcoded URLs. It also reduces coupling: if a resource URL changes, only the server changes — compliant clients follow the new link automatically. In this API, the `GET /api/v1` discovery endpoint demonstrates this by returning a `links` map pointing to `/api/v1/rooms` and `/api/v1/sensors`.

---

### Part 2.1 — IDs vs Full Objects in List Responses

Returning only IDs in a list response minimises payload size, which is beneficial when the collection is large and clients may only need a subset of full details. However, it forces clients to make N additional HTTP requests to fetch each object individually — a classic "N+1 request" problem that significantly increases latency and server load.

Returning full room objects in the list response requires more bandwidth per request but enables clients to render complete UIs or process data in a single round-trip. For a campus management system where rooms have relatively small payloads, returning full objects is the better trade-off. This API returns full objects from `GET /api/v1/rooms`, with the knowledge that sensor detail is provided separately via the nested readings sub-resource.

---

### Part 2.2 — Idempotency of DELETE

The DELETE operation is **idempotent by HTTP specification**, but the specific behaviour depends on implementation. In this API:

- The **first** `DELETE /api/v1/rooms/{roomId}` on an existing, empty room succeeds with `200 OK` and removes the resource.
- A **second identical** `DELETE` request finds no room with that ID and returns `404 Not Found`.

This is correct idempotent behaviour. Idempotency means the **server state** after multiple identical requests is the same as after one — the room remains deleted regardless of how many times the request is sent. The HTTP status code changing from `200` to `404` on subsequent calls does not violate idempotency; the state outcome (room is absent) is identical. If a client accidentally sends the same DELETE twice, no data corruption, no orphaned records, and no unintended side effects occur.

---

### Part 3.1 — @Consumes and Content-Type Mismatches

The `@Consumes(MediaType.APPLICATION_JSON)` annotation declares that the POST endpoint only accepts requests with a `Content-Type: application/json` header. If a client sends data with a different media type — for example `text/plain` or `application/xml` — JAX-RS intercepts the request **before it reaches the resource method** and automatically returns **`415 Unsupported Media Type`**. The resource method body is never executed.

This is handled by the JAX-RS runtime's content negotiation layer. The framework inspects the incoming `Content-Type` header, compares it against all registered `@Consumes` annotations, and rejects mismatches at the infrastructure level. This means developers do not need to manually validate incoming content types inside every method — the framework enforces it declaratively, which is both cleaner and more consistent.

---

### Part 3.2 — @QueryParam vs Path Parameter for Filtering

Using a query parameter (`GET /api/v1/sensors?type=CO2`) is considered superior to embedding the filter in the path (`GET /api/v1/sensors/type/CO2`) for several reasons.

**Semantic correctness:** Path segments identify a specific resource by identity. `/sensors/OCC-001` is correct because `OCC-001` uniquely identifies one sensor. `type=CO2` is a filter over a collection — it does not identify a resource, it narrows a set. Query parameters are semantically designed for this purpose.

**Optionality and composability:** Query parameters are naturally optional. A client can call `GET /api/v1/sensors` for all sensors, or add `?type=CO2` to filter. Multiple filters can be chained (`?type=CO2&status=ACTIVE`) without changing the path structure. Path-based filters (`/sensors/type/CO2/status/ACTIVE`) become deeply nested and awkward.

**Cacheability and REST conventions:** RESTful caching infrastructure (CDNs, proxies) treats the path as the resource identifier. Keeping the path stable (`/sensors`) while varying query parameters maintains clean cache keys for the unfiltered collection.

---

### Part 4.1 — Sub-Resource Locator Pattern

The Sub-Resource Locator pattern delegates responsibility for a sub-path to a separate, dedicated class rather than handling all nested paths in a single resource class. In this API, `SensorResource` contains only a locator method annotated with `@Path("/{sensorId}/readings")` that returns a new `SensorReadingResource` instance. JAX-RS continues method resolution on that returned object, dispatching `GET` and `POST` to the correct methods there.

The architectural benefits are substantial in large APIs. A single "god controller" that handles every nested path (`/sensors`, `/sensors/{id}`, `/sensors/{id}/readings`, `/sensors/{id}/readings/{rid}`) quickly becomes thousands of lines long and difficult to maintain. The sub-resource locator enforces **Single Responsibility Principle**: `SensorResource` manages sensor CRUD, `SensorReadingResource` manages reading history. Each class is independently testable, independently modifiable, and logically coherent. As APIs grow — for example adding `/sensors/{id}/alerts` or `/sensors/{id}/calibrations` — new sub-resource classes are added without touching existing code.

---

### Part 5.2 — HTTP 422 vs 404 for Missing References in Payloads

HTTP `404 Not Found` signals that the **requested URL** does not correspond to any known resource on the server. It describes a problem with the *request target*, not the *request body*.

When a client POSTs a valid JSON payload to `/api/v1/sensors` — a URL that clearly exists — but the `roomId` field inside that payload references a room that does not exist, the URL is valid and the server understood the request perfectly. The problem is a **semantic integrity violation within the payload itself**. HTTP `422 Unprocessable Entity` is semantically precise here: it means "the request was well-formed and understood, but the contained instructions could not be followed due to semantic errors."

Using `404` in this scenario would be misleading — a client receiving `404` would reasonably assume the `/sensors` endpoint does not exist, not that a field value in their JSON body was invalid. `422` communicates the actual problem clearly and accurately, leading to faster debugging and better client-side error handling.

---

### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers is a significant security vulnerability because they contain information that dramatically assists an attacker in crafting targeted exploits:

**Technology fingerprinting:** Stack traces reveal the exact framework names and version numbers (e.g., `org.glassfish.jersey 2.41`, `com.fasterxml.jackson 2.15.2`). Attackers can immediately look up known CVEs for those exact versions and attempt corresponding exploits.

**Internal path disclosure:** Fully-qualified class names and file paths (e.g., `com.smartcampus.store.DataStore.getRoom(DataStore.java:47)`) expose the internal package structure, class naming conventions, and source file layout of the application. This allows attackers to infer architectural patterns and identify likely entry points.

**Logic and data flow exposure:** The call stack in a trace shows the precise sequence of method calls that led to an error. An attacker can reverse-engineer business logic, understand validation order, and identify where checks are absent or fail-open.

**Error condition mapping:** By deliberately triggering different errors and observing traces, an attacker can systematically map the application's error surface — understanding which inputs cause which exceptions — to identify exploitable edge cases.

The Global `ExceptionMapper<Throwable>` in this API ensures that regardless of what goes wrong internally, the external consumer only ever sees a generic `500 Internal Server Error` with a safe message. All detail is logged server-side where only authorised personnel can access it.

---

### Part 5.5 — JAX-RS Filters vs Manual Logging

Using JAX-RS filters for cross-cutting concerns like logging is far superior to inserting `Logger.info()` calls into every resource method for several reasons.

**Separation of concerns:** Logging is infrastructure, not business logic. Resource methods should focus exclusively on handling requests and building responses. Mixing logging code into every method conflates two distinct responsibilities and makes both harder to read and maintain.

**DRY principle:** With 10 resource methods across 3 resource classes, manual logging requires at least 20 log statements (one on entry, one on exit per method). A single filter class handles all of them automatically. Adding a new resource method requires zero logging changes.

**Consistency:** Manual logging is fragile — a developer might forget to add it to a new method, log different information in different methods, or accidentally log at the wrong level. A filter is applied uniformly to every request and response without exception.

**Lifecycle control:** Filters can be registered, deregistered, prioritised, and composed using `@Priority` without touching any resource code. Switching from `java.util.logging` to SLF4J, adding request correlation IDs, or enabling/disabling logging per environment requires changing only the filter class.
