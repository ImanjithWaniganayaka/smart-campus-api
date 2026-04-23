# 🏫 Smart Campus Sensor & Room Management API

A RESTful API built with **Java 11, JAX-RS (Jersey 2.x), and an embedded Grizzly HTTP server** for managing university campus rooms and IoT sensors. This project was developed as coursework for the 5COSC022W Client-Server Architectures module at the University of Westminster.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Technologies Used](#technologies-used)
- [How to Build and Run](#how-to-build-and-run)
- [API Endpoints](#api-endpoints)
- [Sample curl Commands](#sample-curl-commands)
- [Error Handling](#error-handling)
- [Report: Conceptual Questions](#report-conceptual-questions)

---

## 📖 Overview

The Smart Campus API provides a RESTful interface for campus facilities managers to manage **Rooms** and **Sensors** deployed across university buildings. Key capabilities include:

- Create, retrieve, and delete campus rooms
- Register and filter IoT sensors by type
- Record and retrieve sensor readings via nested sub-resources
- Business rule enforcement (e.g. rooms with active sensors cannot be deleted)
- Comprehensive exception handling — the API never exposes raw Java stack traces
- Request and response logging via JAX-RS filters

---

## 🛠️ Technologies Used

| Technology | Purpose |
|------------|---------|
| Java 11 | Core language |
| JAX-RS (Jersey 2.x) | REST framework |
| Grizzly HTTP Server | Embedded server |
| Jackson | JSON serialisation |
| Maven | Build tool |

---

## 🚀 How to Build and Run

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

## 🔗 API Endpoints

### 🔍 Discovery

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1` | Returns API version, contact info, and resource links |

---

### 🏠 Rooms — `/api/v1/rooms`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/rooms` | Retrieve all rooms |
| GET | `/rooms/{id}` | Retrieve a specific room by ID |
| POST | `/rooms` | Create a new room |
| DELETE | `/rooms/{id}` | Delete a room (fails with 409 if sensors are still assigned) |

---

### 📡 Sensors — `/api/v1/sensors`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/sensors` | Retrieve all sensors |
| GET | `/sensors?type=CO2` | Filter sensors by type (optional query parameter) |
| GET | `/sensors/{id}` | Retrieve a specific sensor by ID |
| POST | `/sensors` | Register a new sensor (roomId must exist) |

---

### 📊 Sensor Readings — `/api/v1/sensors/{id}/readings`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/sensors/{id}/readings` | Retrieve all readings for a sensor |
| POST | `/sensors/{id}/readings` | Add a new reading (also updates sensor's `currentValue`) |

---

## 💻 Sample curl Commands

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

## ⚠️ Error Handling

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

## 📝 Report: Conceptual Questions

### ⚙️ Part 1 — Service Architecture & Setup

**Q1: In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.**

Typically, JAX-RS resource classes are set to a per-request lifecycle by default, i.e., a separate object gets instantiated for every single request that comes in. This means that any sharing of class level state is avoided. Still, developers should remember that the internal data structures of the program such as HashMaps or ArrayLists, can potentially be shared across the requests making it possible that they are simultaneously accessed by different threads. Consequently, programmers ought to make their code thread safe by employing synchronization or using thread safe data structures (say, ConcurrentHashMap) to eliminate issues like race conditions, data corruption scenarios or inconsistent updates.

---

**Q2: Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?**

Hypermedia, also known as HATEOAS, is a technique of embedding links inside the responses of an API to indicate to clients what operations they can carry out next. This feature turns the API into a self-explanatory system that can be easily traversed, in fact, it almost eliminates the need for a separate document to explain the API. Besides that, it also raises the level of the API's adaptability because clients interpret links instead of fixed URLs which means developers have more freedom to change the API without rendering client applications unusable.

---

### 🏠 Part 2 — Room Management

**Q3: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.**

Sending back only IDs cuts down on network bandwidth and speeds up the performance, especially when dealing with large datasets, but it means clients will have to make more requests to get the complete details. On the other hand, sending back full room objects gives the clients all the details in a single response, so they don't have to make multiple requests. However, this also means a bigger payload and more processing. The decision really comes down to a trade-off between efficiency and convenience.

---

**Q4: Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.**

DELETE is definitely idempotent. The reason being is if you keep repeating the same request, the end result will be the same. Initially, the room is deleted successfully. After that, if the same DELETE request is sent again, since the room was already removed, the server might respond with 404 Not Found, but no changes will occur anymore. This way, the system's behavior remains consistent.

---

### 📡 Part 3 — Sensor Operations & Linking

**Q5: We explicitly use the `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?**

By annotating with `@Consumes(MediaType.APPLICATION_JSON)`, the endpoint is restricted to accept only JSON formatted data. If a client sends another type like text/plain or XML, JAX-RS won't be able to handle it, and will automatically respond with an HTTP 415 Unsupported Media Type status. This way, uniform data formats are guaranteed, and invalid inputs are avoided.

---

**Q6: You implemented this filtering using `@QueryParam`. Contrast this with an alternative design where the type is part of the URL path (e.g., `/api/v1/sensors/type/CO2`). Why is the query parameter approach generally considered superior for filtering and searching collections?**

Using `@QueryParam` (for example, `/sensors?type=CO2`) is more suitable for filtering purposes since it helps in keeping the endpoint flexible and it even supports multiple optional filters. It visually communicates that the filtering isn't necessary which is a plus point. On the other hand, using path parameters results in URLs being less flexible and it becomes challenging to add more filtering options.

---

### 🔀 Part 4 — Deep Nesting with Sub-Resources

**Q7: Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., `sensors/{id}/readings/{rid}`) in one massive controller class?**

The Sub-Resource Locator pattern enhances API design by dividing tasks among different classes. Rather than managing all the nested paths in a single big controller, each resource (for instance, sensor readings) is controlled by its own class. This results in code that is not only more modular, readable, and maintainable but also simpler to extend when the API is expanded.

---

### 🛡️ Part 5 — Advanced Error Handling, Exception Mapping & Logging

**Q8: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?**

HTTP 422 really fits best because it means that the request has a perfect syntax (in other words, it is a well-formed JSON), but the data inside it are wrong, for example, the roomId does not exist. The server in principle gets the message but cannot execute it due to logical errors. On the other hand, HTTP 404 stands for the fact that the resource you want is not found at all — it does not mean that the problem is in the request data. So 422 is a more appropriate status code for the case of validation errors in a valid request.

---

**Q9: From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?**

By revealing Java stack traces, one discloses excessively to the attackers as the trace can contain information about class names, file directories, used frameworks, and internal logic of the application. This knowledge can help the attacker to discover a weak point and to focus the attack on the part of the system. The main way to prevent this is ensuring that APIs only display generic error messages and keep detailed error reports in internal logs.

---
**Q10: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?**

One of the benefits of employing JAX-RS filters in the logging process is that they manage the core issues centrally i.e., they eliminate the need to embed logging code on each resource method and hence the code is cleaner, easier to maintain and less prone to errors as developers don't have to copy-paste Logger.info() statements across the API.
Besides, filters facilitate consistency because not a single request or response gets logged in a dissimilar manner. Moreover, if there are alterations in the logging performance, they can be reflected without touching a single file. The neatest feature of this style of work includes the fact that it is highly reasonable and it does not violate the principles of good design e.g. the separation of concerns.

*University of Westminster – 5COSC022W Client-Server Architectures – 2025/26*
