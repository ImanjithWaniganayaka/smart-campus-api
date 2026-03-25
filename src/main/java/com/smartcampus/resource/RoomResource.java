package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Part 2 – Room Management
 * Manages the /api/v1/rooms resource collection.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * Part 2.1 – GET /api/v1/rooms
     * Returns all rooms with full object details.
     */
    @GET
    public Response getAllRooms() {
        List<Room> rooms = new ArrayList<>(store.getRooms().values());
        return Response.ok(rooms).build();
    }

    /**
     * Part 2.1 – POST /api/v1/rooms
     * Creates a new room. Returns 201 Created with Location header.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", 400, "error", "Bad Request", "message", "Room 'id' is required."))
                    .build();
        }
        if (store.getRoom(room.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("status", 409, "error", "Conflict", "message", "Room with id '" + room.getId() + "' already exists."))
                    .build();
        }
        store.addRoom(room);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Room created successfully.");
        response.put("room", room);
        response.put("link", "/api/v1/rooms/" + room.getId());

        return Response.status(Response.Status.CREATED)
                .header("Location", "/api/v1/rooms/" + room.getId())
                .entity(response)
                .build();
    }

    /**
     * Part 2.1 – GET /api/v1/rooms/{roomId}
     * Returns a single room by ID.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            throw new NotFoundException("Room with id '" + roomId + "' not found.");
        }
        return Response.ok(room).build();
    }

    /**
     * Part 2.2 – DELETE /api/v1/rooms/{roomId}
     * Deletes a room ONLY if it has no sensors assigned.
     * Throws RoomNotEmptyException (→ 409) if sensors still exist.
     *
     * Idempotency: DELETE is idempotent in HTTP semantics — calling it
     * multiple times on the same resource should not cause errors beyond the
     * first call. The second call returns 404 (resource gone), which is the
     * correct idempotent behaviour.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            throw new NotFoundException("Room with id '" + roomId + "' not found.");
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }
        store.deleteRoom(roomId);
        return Response.ok(Map.of(
                "message", "Room '" + roomId + "' has been successfully decommissioned.",
                "deletedRoomId", roomId
        )).build();
    }
}
