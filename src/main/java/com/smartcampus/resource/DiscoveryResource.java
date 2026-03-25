package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Part 1.2 – Discovery Endpoint
 * GET /api/v1 → returns API metadata including versioning, contact, and resource links (HATEOAS).
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> info = new HashMap<>();
        info.put("api", "Smart Campus Sensor & Room Management API");
        info.put("version", "1.0.0");
        info.put("description", "RESTful API for managing university campus rooms and IoT sensors.");
        info.put("contact", Map.of(
                "name", "Smart Campus Team",
                "email", "admin@smartcampus.ac.uk"
        ));

        // HATEOAS-style resource links — clients can discover endpoints dynamically
        info.put("resources", Map.of(
                "rooms",    "/api/v1/rooms",
                "sensors",  "/api/v1/sensors"
        ));

        info.put("links", Map.of(
                "self",     "/api/v1",
                "rooms",    "/api/v1/rooms",
                "sensors",  "/api/v1/sensors"
        ));

        return Response.ok(info).build();
    }
}
