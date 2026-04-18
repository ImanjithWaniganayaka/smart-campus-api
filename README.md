# Smart Campus Sensor & Room Management API

A RESTful API built with **Java 11, JAX-RS (Jersey 2.x), and an embedded Grizzly HTTP server** for managing university campus rooms and IoT sensors. This project was developed as coursework for the 5COSC022W Client-Server Architectures module at the University of Westminster.

---

## Table of Contents

- [Overview](#overview)
- [Technologies Used](#technologies-used)
- [How to Build and Run](#how-to-build-and-run)
- [API Endpoints](#api-endpoints)
- [Sample curl Commands](#sample-curl-commands)
- [Error Handling](#error-handling)
- [Report: Conceptual Questions](#report-conceptual-questions)

---

## Overview

The Smart Campus API provides a RESTful interface for campus facilities managers to manage **Rooms** and **Sensors** deployed across university buildings. Key capabilities include:

- Create, retrieve, and delete campus rooms
- Register and filter IoT sensors by type
- Record and retrieve sensor readings via nested sub-resources
- Business rule enforcement (e.g. rooms with active sensors cannot be deleted)
- Comprehensive exception handling — the API never exposes raw Java stack traces
- Request and response logging via JAX-RS filters
- In-memory data storage using `HashMap` and `ArrayList` (no database)

---

## Technologies Used

| Technology | Purpose |
|------------|---------|
| Java 11 | Core language |
| JAX-RS (Jersey 2.x) | REST framework |
| Grizzly HTTP Server | Embedded server |
| Jackson | JSON serialisation |
| Maven | Build tool |

---

## How to Build and Run

### Prerequisites

- Java 11 or higher (`java -version`)
- Apache Maven 3.6 or higher (`mvn -version`)

### 1. Clone the repository

```bash
git clone https://github.com/ImanjithWaniganayaka/smart-campus-api.git
cd smart-campus-api
```

### 2. Build the project

```bash
mvn clean package
```

This compiles the source code and produces a fat JAR in the `target/` directory.

### 3. Run the server

```bash
java -jar target/smart-campus-api-1.0.0.jar
```

The server starts on **port 8081**. You should see a confirmation message in the console.

### 4. Verify it is running

Open your browser or run:

```bash
curl http://localhost:8081/api/v1/
```

You should receive a JSON discovery response with available resource links.

### Base URL

```
http://localhost:8081/api/v1
```

---

## API Endpoints

### Discovery

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1` | Returns API version, contact info, and resource links |

---

### Rooms — `/api/v1/rooms`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/rooms` | Retrieve all rooms |
| GET | `/rooms/{id}` | Retrieve a specific room by ID |
| POST | `/rooms` | Create a new room |
| DELETE | `/rooms/{id}` | Delete a room (fails with 409 if sensors are still assigned) |

---

### Sensors — `/api/v1/sensors`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/sensors` | Retrieve all sensors |
| GET | `/sensors?type=CO2` | Filter sensors by type (optional query parameter) |
| GET | `/sensors/{id}` | Retrieve a specific sensor by ID |
| POST | `/sensors` | Register a new sensor (roomId must exist) |

---

### Sensor Readings — `/api/v1/sensors/{id}/readings`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/sensors/{id}/readings` | Retrieve all readings for a sensor |
| POST | `/sensors/{id}/readings` | Add a new reading (also updates sensor's `currentValue`) |

---

## Sample curl Commands

### 1. Discover the API

```bash
curl http://localhost:8081/api/v1/
```

### 2. Create a room

```bash
curl -X POST http://localhost:8081/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'
```

### 3. Create a sensor linked to that room

```bash
curl -X POST http://localhost:8081/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'
```

### 4. Filter sensors by type

```bash
curl "http://localhost:8081/api/v1/sensors?type=CO2"
```

### 5. Add a sensor reading

```bash
curl -X POST http://localhost:8081/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":412.5}'
```

### 6. Retrieve all readings for a sensor

```bash
curl http://localhost:8081/api/v1/sensors/CO2-001/readings
```

### 7. Attempt to delete a room that has sensors (expect 409 Conflict)

```bash
curl -X DELETE http://localhost:8081/api/v1/rooms/LIB-301
```

### 8. Attempt to create a sensor with a non-existent roomId (expect 422)

```bash
curl -X POST http://localhost:8081/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-999"}'
```

---

## Error Handling

The API is designed to never expose raw Java stack traces. All errors return a structured JSON body.

| Status Code | Scenario |
|-------------|----------|
| 400 Bad Request | Malformed or missing required fields |
| 403 Forbidden | Attempted to post a reading to a sensor in MAINTENANCE status |
| 404 Not Found | Requested resource does not exist |
| 409 Conflict | Attempted to delete a room that still has sensors assigned |
| 415 Unsupported Media Type | Request body sent with wrong Content-Type |
| 422 Unprocessable Entity | Sensor created with a `roomId` that does not exist |
| 500 Internal Server Error | Unexpected runtime error (caught by global exception mapper) |

---

## Report: Conceptual Questions

### Part 1 — Service Architecture & Setup

#### Q1.1: JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (per-request scope). This is the default lifecycle defined by the JAX-RS specification, meaning the runtime does not reuse the same object across requests.

This design decision has significant implications for in-memory data management. Because each resource instance is fresh per request, any instance variables on the resource class itself would be lost at the end of each request — they cannot be used to persist data across calls. To work around this, shared state (such as the room and sensor maps) must be stored outside of the resource class, in a static data store or a singleton utility class. In this implementation, a dedicated `DataStore` class with static `HashMap` and `ArrayList` fields holds all data. Since static fields are shared across all instances, they survive across requests. However, this also means that concurrent requests from multiple threads could read and write to the same collections simultaneously, creating race conditions. To prevent data corruption, access to these shared collections must be synchronised — either by using `ConcurrentHashMap` instead of `HashMap`, or by wrapping critical sections with `synchronized` blocks.

#### Q1.2: HATEOAS — Hypermedia as the Engine of Application State

HATEOAS (Hypermedia as the Engine of Application State) is a constraint of the REST architectural style where API responses include hyperlinks that tell the client what actions or resources are available next — embedded directly in the response body rather than documented externally.

For example, a response to `GET /rooms/LIB-301` might include a link to `GET /rooms/LIB-301/sensors` and `DELETE /rooms/LIB-301`, so the client does not need to guess or hard-code URL patterns. This is considered a hallmark of advanced REST design because it makes the API **self-describing and discoverable**: a client can navigate the entire API by following links, much like a human navigates a website. Compared to static documentation, HATEOAS allows the server to evolve its URL structure without breaking clients — clients always follow links from responses rather than constructing URLs from a separate spec. This reduces coupling between client and server and dramatically lowers the cost of API changes.

---

### Part 2 — Room Management

#### Q2.1: Returning Room IDs vs Full Room Objects

When returning a list of rooms, there is a meaningful trade-off between returning only IDs versus returning full room objects.

**Returning only IDs** reduces the size of each list response significantly — useful when there are thousands of rooms. However, if the client needs any detail about the rooms (names, capacities, sensor counts), it must issue a separate `GET /rooms/{id}` request for each entry, resulting in the N+1 request problem. This increases latency and server load considerably.

**Returning full objects** sends more data per response but allows the client to render a full room listing in a single request. For typical use cases — such as a facilities manager dashboard displaying all room names and capacities — this is far more efficient. The trade-off is higher bandwidth per request, but given that room objects are small, this is generally the better default. In this implementation, full room objects are returned in the list response to minimise client round-trips.

#### Q2.2: Idempotency of DELETE

Yes, the `DELETE /rooms/{id}` operation is **idempotent** in this implementation. The HTTP specification requires that DELETE be idempotent — meaning that sending the same request multiple times should produce the same server state as sending it once.

In practice: the first `DELETE /rooms/LIB-301` removes the room from the data store and returns `200 OK`. Any subsequent `DELETE /rooms/LIB-301` request finds no room with that ID and returns a `404 Not Found` response. While the HTTP status code differs between the first and subsequent calls, the **server state is identical** after each call — the room does not exist in either case. This satisfies the idempotency requirement because the effect on the resource is the same regardless of how many times the request is repeated. The response body or status code may vary, but the data state does not.

---

### Part 3 — Sensor Operations & Linking

#### Q3.1: Behaviour When a Client Sends the Wrong Content-Type

When a POST endpoint is annotated with `@Consumes(MediaType.APPLICATION_JSON)`, JAX-RS uses **content negotiation** to match the incoming request's `Content-Type` header against the types the method can consume.

If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, the JAX-RS runtime will reject the request **before the method is even invoked** and automatically return an **HTTP 415 Unsupported Media Type** response. This is handled entirely by the framework — no application code is needed to produce this error. The client is informed that the server cannot process the entity in the format they provided. This is an important safeguard because attempting to deserialise XML or plain text as JSON would result in a parsing error or corrupt data, so JAX-RS blocks the request early. The custom `ExceptionMapper` for general exceptions acts as a safety net for any edge cases that slip through, but the 415 response is a first-class framework concern.

#### Q3.2: @QueryParam Filtering vs Path-Based Filtering

Using `@QueryParam` for filtering (e.g. `GET /sensors?type=CO2`) is generally considered superior to embedding the filter in the path (e.g. `GET /sensors/type/CO2`) for several reasons.

First, **semantic clarity**: the path should identify a resource, while query parameters refine or filter a collection. `/sensors` refers to the sensors collection; `?type=CO2` is a modifier on that collection. A path like `/sensors/type/CO2` implies that `type/CO2` is itself a distinct resource, which is misleading.

Second, **flexibility and composability**: query parameters can be combined freely (e.g. `?type=CO2&status=ACTIVE`), whereas nesting filters in the path quickly becomes unwieldy and requires separate route definitions for every combination.

Third, **caching and REST conventions**: standard HTTP caching infrastructure understands query strings as variations of the same resource. The query parameter approach aligns with how search and filtering are handled universally across REST APIs and is consistent with what client developers expect.

---

### Part 4 — Deep Nesting with Sub-Resources

#### Q4.1: Benefits of the Sub-Resource Locator Pattern

The Sub-Resource Locator pattern involves a method in a parent resource class (e.g. `SensorResource`) that returns an instance of a child resource class (e.g. `SensorReadingResource`) rather than handling the nested path directly. JAX-RS then delegates further routing to that child class.

The main architectural benefit is **separation of concerns and maintainability**. In a large API, defining every nested route in a single controller class would create a monolithic, hard-to-navigate class that violates the Single Responsibility Principle. By delegating `/sensors/{id}/readings` logic to a dedicated `SensorReadingResource`, each class is focused on one resource. This makes the code easier to read, test, and modify independently. Changes to how readings are stored or validated do not require touching the sensor listing or creation logic.

Additionally, the child resource class receives the sensor context (the `sensorId`) at instantiation time, meaning it always operates within the correct scope. This is cleaner than passing IDs through every method signature in a shared controller, and it scales gracefully as the API grows.

---

### Part 5 — Advanced Error Handling, Exception Mapping & Logging

#### Q5.2: Why HTTP 422 is More Semantically Accurate Than 404 for a Missing roomId Reference

When a client sends a `POST /sensors` request with a `roomId` that does not exist, the request payload itself is syntactically valid JSON and arrives at the server correctly — it is not a missing or malformed request. The issue is a **semantic validation failure**: the referenced entity (`roomId`) does not exist within the system.

HTTP 404 Not Found is semantically reserved for cases where **the resource being requested** — i.e. the URL itself — cannot be found. In this case, the URL (`/sensors`) exists and is correct; the problem is a broken reference inside the request body.

HTTP 422 Unprocessable Entity was designed precisely for this scenario: the server understands the content type and the request is well-formed, but it **cannot be acted upon because of semantic errors** in the data. Returning 422 communicates to the client that the request structure was fine but the referenced `roomId` value is logically invalid, giving developers a much clearer signal about where the problem lies and how to fix it. This leads to better API ergonomics and reduces debugging time.

#### Q5.4: Cybersecurity Risks of Exposing Java Stack Traces

Exposing raw Java stack traces to external API consumers is a significant security risk for several reasons.

**Information disclosure**: A stack trace reveals the internal package structure, class names, method names, and line numbers of the application. An attacker can use this to map the codebase, identify frameworks in use (e.g. Jersey, Jackson versions), and target known vulnerabilities in those specific versions.

**Attack surface mapping**: Stack traces from `NullPointerException` or `IndexOutOfBoundsException` can reveal exactly which data paths cause failures, allowing an attacker to craft inputs that deliberately trigger unstable code paths or probe for injection vulnerabilities.

**Dependency fingerprinting**: The presence of third-party library classes in a trace (e.g. `com.fasterxml.jackson...`) tells an attacker precisely which libraries and versions are running, enabling targeted CVE exploitation.

**Operational intelligence**: File paths and server directory structures sometimes appear in traces, giving attackers knowledge of the deployment environment.

The correct approach — implemented in this API via a global `ExceptionMapper<Throwable>` — is to log the full stack trace server-side for debugging, while returning only a generic, opaque error message to the client (e.g. `"An unexpected internal error occurred"`).

#### Q5.5: Why JAX-RS Filters Are Preferable to Manual Logging in Resource Methods

Using JAX-RS filters (implementing `ContainerRequestFilter` and `ContainerResponseFilter`) for cross-cutting concerns like logging is a significantly better design than manually inserting `Logger.info()` calls into every resource method.

**Avoidance of code duplication**: With manual logging, every resource method must include logging statements. As the API grows across dozens of endpoints, this creates hundreds of duplicated lines that must all be maintained consistently. A single filter applies to all requests automatically.

**Separation of concerns**: Logging is an infrastructural concern, not a business concern. Resource methods should focus on their domain logic — managing rooms, sensors, and readings. Mixing logging code into these methods violates the Single Responsibility Principle and makes the business logic harder to read.

**Consistency**: A filter guarantees that every request and response is logged in exactly the same format, regardless of which developer wrote which endpoint. Manual logging is prone to inconsistency and omission.

**Maintainability**: If the logging format needs to change (e.g. adding a correlation ID or timestamp), only the filter class needs updating — not every resource method in the codebase.

This principle extends to other cross-cutting concerns such as authentication, CORS headers, and request validation, all of which are naturally handled by filters in JAX-RS.

---

## Coursework Information

- **Module:** 5COSC022W – Client-Server Architectures
- **Assignment:** Smart Campus REST API (60% of final grade)
- **Student:** Imanjith Waniganayaka
- **Institution:** University of Westminster
- 
