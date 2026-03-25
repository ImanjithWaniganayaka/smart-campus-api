package com.smartcampus.exception;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 409 Conflict — Room still has sensors; cannot be deleted.
 */
@Provider
class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 409);
        error.put("error", "Conflict");
        error.put("message", ex.getMessage());
        error.put("hint", "Remove or reassign all sensors from room '" + ex.getRoomId() + "' before deleting it.");
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}

/**
 * 422 Unprocessable Entity — Referenced roomId does not exist.
 */
@Provider
class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 422);
        error.put("error", "Unprocessable Entity");
        error.put("message", ex.getMessage());
        error.put("hint", "Ensure the roomId in your request body refers to an existing room.");
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}

/**
 * 403 Forbidden — Sensor is in MAINTENANCE; cannot accept readings.
 */
@Provider
class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 403);
        error.put("error", "Forbidden");
        error.put("message", ex.getMessage());
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}

/**
 * 404 Not Found — Standard JAX-RS NotFoundException wrapper.
 */
@Provider
class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
    @Override
    public Response toResponse(NotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 404);
        error.put("error", "Not Found");
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "The requested resource was not found.");
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}

/**
 * 500 Global Safety Net — Catches any unexpected Throwable.
 * Prevents raw Java stack traces from leaking to API consumers.
 */
@Provider
class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by global mapper", ex);
        Map<String, Object> error = new HashMap<>();
        error.put("status", 500);
        error.put("error", "Internal Server Error");
        error.put("message", "An unexpected error occurred. Please contact the system administrator.");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
