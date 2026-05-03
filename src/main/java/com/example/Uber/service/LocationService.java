package com.example.Uber.service;

import com.example.Uber.dto.DriverLocationDTO;

import java.util.List;

public interface LocationService {
    boolean saveDriverLocation(String driverId, double latitude, double longitude);
    List<DriverLocationDTO> getNearbyDrivers(double latitude, double longitude, double radius);
}
