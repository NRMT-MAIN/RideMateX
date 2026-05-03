package com.example.Uber.service.impl;

import com.example.Uber.dto.DriverLocationDTO;
import com.example.Uber.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisLocationServiceImpl implements LocationService {
    private static final String REDIS_KEY_PREFIX = "driver_location:";
    private final StringRedisTemplate stringRedisTemplate ;

    @Override
    public boolean saveDriverLocation(String driverId, double latitude, double longitude) {
        GeoOperations<String, String> geoOps = stringRedisTemplate.opsForGeo();
        String redisKey = REDIS_KEY_PREFIX + driverId;
        geoOps.add(redisKey,
                new RedisGeoCommands.GeoLocation<>(driverId , new Point(latitude , longitude)));
        return true;
    }

    @Override
    public List<DriverLocationDTO> getNearbyDrivers(double latitude, double longitude, double radius) {
        GeoOperations<String, String> geoOps = stringRedisTemplate.opsForGeo();

        Distance searchRadius = new Distance(radius, Metrics.KILOMETERS);
        Circle searchArea = new Circle(new Point(latitude, longitude), searchRadius);

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResult = geoOps.radius(REDIS_KEY_PREFIX , searchArea) ;

        List<DriverLocationDTO> driverLocations = new ArrayList<>() ;

        for(GeoResult<RedisGeoCommands.GeoLocation<String>> result : geoResult) {
            Point point = geoOps.position(REDIS_KEY_PREFIX, result.getContent().getName()).get(0); // location of individual driver in redis

            DriverLocationDTO driverLocation = DriverLocationDTO.builder()
                    .driverId(geoResult.getContent().toString())
                    .latitude(point.getY())
                    .longitude(point.getX())
                    .build();

            driverLocations.add(driverLocation);
        }

        return driverLocations;
    }
}
