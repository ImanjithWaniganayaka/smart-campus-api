# 🏫 Smart Campus Sensor & Room Management API

A RESTful API built using **Java (JAX-RS with Jersey + Grizzly)** for managing university campus rooms and IoT sensors.

This project demonstrates core REST principles including resource modeling, HTTP methods, status codes, filtering, and sub-resource routing.

---

## 🚀 Features

- 📡 Manage **Rooms** and **Sensors**
- 🔗 Link sensors to rooms
- 📊 Retrieve sensor data and room details
- 🔍 Filter sensors using query parameters
- 🧩 Nested resource structure (`/sensors/{id}/readings`)
- ⚠️ Custom exception handling with proper HTTP status codes
- 📝 Logging using request & response filters
- 💾 In-memory data storage (no database)

---

## 🛠️ Technologies Used

- Java 11+
- JAX-RS (Jersey)
- Grizzly HTTP Server
- Maven

---

## ▶️ How to Run

### 1. Clone the repository
```bash
git clone https://github.com/ImanjithWaniganayaka/smart-campus-api.git
cd smart-campus-api
```

### 2. Build the project
```bash
mvn clean package
```

### 3. Run the application
```bash
java -jar target/smart-campus-api-1.0.0.jar
```

### 4. Open in browser
```bash
[java -jar target/smart-campus-api-1.0.0.jar](http://localhost:8081/api/v1/)
```

### 🌐 Base URL
```bash
[java -jar target/smart-campus-api-1.0.0.jar](http://localhost:8081/api/v1)
```

## 📌 API Endpoints

### 🔹 Discovery
- **GET** `/api/v1`

---

### 🔹 Rooms

| Method | Endpoint        | Description        |
|--------|----------------|------------------|
| GET    | /rooms         | Get all rooms     |
| GET    | /rooms/{id}    | Get room by ID    |
| POST   | /rooms         | Create a new room |
| DELETE | /rooms/{id}    | Delete a room     |

---

### 🔹 Sensors

| Method | Endpoint              | Description              |
|--------|----------------------|--------------------------|
| GET    | /sensors             | Get all sensors          |
| GET    | /sensors?type=CO2    | Filter sensors by type   |
| GET    | /sensors/{id}        | Get sensor by ID         |
| POST   | /sensors             | Create a sensor          |

---

### 🔹 Sensor Readings (Nested Resource)

| Method | Endpoint                      | Description                |
|--------|------------------------------|----------------------------|
| GET    | /sensors/{id}/readings       | Get readings for a sensor  |
| POST   | /sensors/{id}/readings       | Add new reading            |

---

## 🧪 Example cURL Commands

### Get all rooms
```bash
curl http://localhost:8081/api/v1/rooms
```
### Get all sensors
```bash
curl http://localhost:8081/api/v1/sensors
```

### Filter sensors by type
```
curl "http://localhost:8081/api/v1/sensors?type=CO2"
```

### Create a room
```
curl -X POST http://localhost:8081/api/v1/rooms \
-H "Content-Type: application/json" \
-d '{"id":"LAB-102","name":"AI Lab","capacity":40}'
```

### Create a sensor
```
curl -X POST http://localhost:8081/api/v1/sensors \
-H "Content-Type: application/json" \
-d '{"id":"TEMP-002","type":"Temperature","roomId":"LAB-102"}'
```

## ⚠️ Error Handling

The API uses proper HTTP status codes:

- **200 OK** → Success  
- **201 Created** → Resource created  
- **400 Bad Request** → Invalid input  
- **403 Forbidden** → Operation not allowed  
- **404 Not Found** → Resource not found  
- **409 Conflict** → Business rule violation  
- **500 Internal Server Error** → Unexpected error  

---

## 🧠 Design Decisions

- Uses in-memory storage (**HashMap**, **List**) as required by coursework  
- Follows RESTful design principles  
- Uses sub-resource locators for nested routes  
- Clean separation between resources and models  

---

## 📹 Demo

👉 Add your video link here

---

## 📚 Coursework Information

- **Module:** 5COSC022W – Client Server Architecture  
- **Assignment:** Smart Campus REST API  
- **Student:** Imanjith Waniganayaka  

---

## 📌 Author

**Imanjith Waniganayaka**  
Computer Science Undergraduate  
University of Westminster  

---

## 🔥 What you should do now

1. Paste this into `README.md`  
2. Replace the demo link  
3. Push to GitHub  

---

## 💡 If you want extra marks

I can also:

- Add API diagrams  
- Improve wording for distinction level  
- Check your repo against the rubric  

Just tell me 👍
