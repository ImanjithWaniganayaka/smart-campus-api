package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Part 3 – Sensor Operations & Linking
 * Part 4.1 – Sub-Resource Locator Pattern
 *
 * Manages /api/v1/sensors and delegates readings to SensorReadingResource.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * Part 3.2 – GET /api/v1/sensors?type=CO2
     * Returns all sensors, optionally filtered by ?type= query parameter.
     * Using @QueryParam rather than a path segment keeps the URL clean for
     * filtering — path parameters are for identity, query params for filtering.
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(store.getSensors().values());
        if (type != null && !type.isBlank()) {
            result = result.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        return Response.ok(result).build();
    }

    /**
     * Part 3.1 – POST /api/v1/sensors
     * Registers a new sensor. Validates that the referenced roomId exists.
     * Throws LinkedResourceNotFoundException (→ 422) if the room is not found.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", 400, "error", "Bad Request", "message", "Sensor 'id' is required."))
                    .build();
        }
        if (store.getSensor(sensor.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("status", 409, "error", "Conflict", "message", "Sensor with id '" + sensor.getId() + "' already exists."))
                    .build();
        }
        // Referential integrity check — roomId must exist
        if (sensor.getRoomId() == null || store.getRoom(sensor.getRoomId()) == null) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: roomId '" + sensor.getRoomId() + "' does not exist in the system."
            );
        }
        // Default status if not supplied
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }
        store.addSensor(sensor);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensor registered successfully.");
        response.put("sensor", sensor);
        response.put("link", "/api/v1/sensors/" + sensor.getId());

        return Response.status(Response.Status.CREATED)
                .header("Location", "/api/v1/sensors/" + sensor.getId())
                .entity(response)
                .build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     * Returns a single sensor by ID.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor with id '" + sensorId + "' not found.");
        }
        return Response.ok(sensor).build();
    }

    /**
     * DELETE /api/v1/sensors/{sensorId}
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        if (store.getSensor(sensorId) == null) {
            throw new NotFoundException("Sensor with id '" + sensorId + "' not found.");
        }
        store.deleteSensor(sensorId);
        return Response.ok(Map.of("message", "Sensor '" + sensorId + "' deleted.", "deletedSensorId", sensorId)).build();
    }

    /**
     * Part 4.1 – Sub-Resource Locator
     * GET/POST /api/v1/sensors/{sensorId}/readings
     *
     * Instead of defining every nested path here (leading to a bloated controller),
     * this locator delegates to a dedicated SensorReadingResource class. JAX-RS
     * runtime continues method resolution on the returned instance. This pattern
     * keeps each class focused on a single responsibility and drastically simplifies
     * large APIs with deep nesting.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
