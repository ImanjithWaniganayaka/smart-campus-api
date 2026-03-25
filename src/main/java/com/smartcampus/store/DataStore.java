package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe singleton acting as the in-memory database.
 *
 * Because JAX-RS resource classes are request-scoped (a new instance is
 * created per request), all shared state MUST live outside individual
 * resource instances. This singleton uses ConcurrentHashMap so that
 * concurrent requests do not corrupt data or cause race conditions.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    // ConcurrentHashMap provides thread-safe read/write without manual synchronization
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {
        seedData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ── Rooms ──────────────────────────────────────────────────────────────────

    public Map<String, Room> getRooms() { return rooms; }

    public Room getRoom(String id) { return rooms.get(id); }

    public void addRoom(Room room) { rooms.put(room.getId(), room); }

    public boolean deleteRoom(String id) {
        return rooms.remove(id) != null;
    }

    // ── Sensors ────────────────────────────────────────────────────────────────

    public Map<String, Sensor> getSensors() { return sensors; }

    public Sensor getSensor(String id) { return sensors.get(id); }

    public void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        // Link sensor ID to its room
        Room room = rooms.get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().add(sensor.getId());
        }
        // Initialise an empty readings list for this sensor
        readings.putIfAbsent(sensor.getId(), new ArrayList<>());
    }

    public boolean deleteSensor(String id) {
        Sensor sensor = sensors.remove(id);
        if (sensor != null) {
            // Unlink from room
            Room room = rooms.get(sensor.getRoomId());
            if (room != null) {
                room.getSensorIds().remove(id);
            }
            readings.remove(id);
            return true;
        }
        return false;
    }

    // ── Readings ───────────────────────────────────────────────────────────────

    public List<SensorReading> getReadings(String sensorId) {
        return readings.getOrDefault(sensorId, new ArrayList<>());
    }

    public synchronized void addReading(String sensorId, SensorReading reading) {
        readings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
        // Side-effect: update parent sensor's currentValue
        Sensor sensor = sensors.get(sensorId);
        if (sensor != null) {
            sensor.setCurrentValue(reading.getValue());
        }
    }

    // ── Seed Data ──────────────────────────────────────────────────────────────

    private void seedData() {
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Science Lab", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 412.0, "LIB-301");
        Sensor s3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 0.0, "LAB-101");

        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        r1.getSensorIds().add(s1.getId());
        r1.getSensorIds().add(s2.getId());
        r2.getSensorIds().add(s3.getId());

        readings.put(s1.getId(), new ArrayList<>());
        readings.put(s2.getId(), new ArrayList<>());
        readings.put(s3.getId(), new ArrayList<>());
    }
}
