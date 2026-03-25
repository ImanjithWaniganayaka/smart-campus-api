package com.smartcampus.exception;

public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String sensorId) {
        super("Sensor " + sensorId + " is currently in MAINTENANCE and cannot accept new readings.");
    }
}
