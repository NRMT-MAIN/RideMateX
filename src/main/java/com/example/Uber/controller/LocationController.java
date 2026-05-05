package com.example.Uber.controller;

import com.example.Uber.dto.DriverLocationDTO;
import com.example.Uber.dto.NearbyDriversRequestDTO;
import com.example.Uber.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService ;

    @PostMapping("/driverLocation")
    public ResponseEntity<Boolean> saveDriverLocation(@RequestBody DriverLocationDTO driverLocation) {
        Boolean saved = locationService.saveDriverLocation(driverLocation.getDriverId().intValue(), driverLocation.getLatitude(), driverLocation.getLongitude());
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/nearbyDrivers")
    public ResponseEntity<List<DriverLocationDTO>> getNearbyDrivers(@RequestBody NearbyDriversRequestDTO request) {
        List<DriverLocationDTO> nearbyDrivers = locationService.getNearbyDrivers(request.getLatitude(), request.getLongitude(), request.getRadius());
        return ResponseEntity.ok(nearbyDrivers);
    }
}
