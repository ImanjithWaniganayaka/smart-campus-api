package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Part 4.2 – Historical Data Management
 * Sub-resource for /api/v1/sensors/{sensorId}/readings
 *
 * This class is instantiated by SensorResource via the sub-resource locator
 * pattern, receiving the sensorId as context. Separating logic into its own
 * class keeps SensorResource clean and makes this class independently testable.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns all historical readings for this sensor.
     */
    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor with id '" + sensorId + "' not found.");
        }
        List<SensorReading> history = store.getReadings(sensorId);
        Map<String, Object> response = new HashMap<>();
        response.put("sensorId", sensorId);
        response.put("count", history.size());
        response.put("readings", history);
        return Response.ok(response).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Appends a new reading. Blocked if sensor is in MAINTENANCE (→ 403).
     * Side effect: updates the parent sensor's currentValue field.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor with id '" + sensorId + "' not found.");
        }
        // Part 5.3 – State Constraint: block MAINTENANCE sensors
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }
        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", 400, "error", "Bad Request", "message", "Reading body is required."))
                    .build();
        }
        // Assign a UUID and timestamp if not provided
        SensorReading newReading = new SensorReading(reading.getValue());

        // Persist and update parent sensor's currentValue
        store.addReading(sensorId, newReading);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Reading recorded successfully.");
        response.put("reading", newReading);
        response.put("updatedSensorValue", sensor.getCurrentValue());

        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
