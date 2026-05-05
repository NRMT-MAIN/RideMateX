package com.example.Uber.service;

import com.example.Uber.dto.DriverLocationDTO;

import java.util.List;

public interface LocationService {

    Boolean saveDriverLocation(Integer driverId, Double latitude, Double longitude);

    List<DriverLocationDTO> getNearbyDrivers(Double latitude, Double longitude, Double radius);
}
