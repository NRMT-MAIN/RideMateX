package com.example.Uber.service.impl;

import com.example.Uber.client.GrpcClient;
import com.example.Uber.dto.BookingRequest;
import com.example.Uber.dto.BookingResponse;
import com.example.Uber.dto.DriverLocationDTO;
import com.example.Uber.entity.Booking;
import com.example.Uber.entity.Driver;
import com.example.Uber.entity.Passenger;
import com.example.Uber.mapper.BookingMapper;
import com.example.Uber.repository.BookingRepository;
import com.example.Uber.repository.DriverRepository;
import com.example.Uber.repository.PassengerRepository;
import com.example.Uber.service.BookingService;
import com.example.Uber.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final BookingMapper bookingMapper;
    private final LocationService locationService;
    private final GrpcClient grpcClient;

    @Override
    @Transactional(readOnly = true)
    public Optional<BookingResponse> findById(Long id) {
        return bookingRepository.findById(id)
                .map(bookingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> findAll() {
        return bookingRepository.findAll().stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> findByPassengerId(Long passengerId) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found with id: " + passengerId));
        return bookingRepository.findByPassenger(passenger).stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> findByDriverId(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + driverId));
        return bookingRepository.findByDriver(driver).stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BookingResponse create(BookingRequest request) {
        Passenger passenger = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found with id: " + request.getPassengerId()));

        String pickupLat = request.getPickupLocationLatitude() != null ?
                request.getPickupLocationLatitude().toString() : "N/A";
        String pickupLon = request.getPickupLocationLongitude() != null ?
                request.getPickupLocationLongitude().toString() : "N/A";

        if("N/A".equals(pickupLat) || "N/A".equals(pickupLon)) {
            throw new IllegalArgumentException("Pickup location is required for booking");
        }

        BigDecimal fare = request.getFare() != null ? request.getFare() : BigDecimal.ZERO;

        // Create booking WITHOUT assigning a driver yet
        // Driver will be assigned when they accept the ride
        Booking newBooking = Booking.builder()
                .passenger(passenger)
                .driver(null)  // No driver assigned yet
                .pickupLocationLatitude(pickupLat)
                .pickupLocationLongitude(pickupLon)
                .dropoffLocation(request.getDropoffLocation())
                .fare(fare)
                .scheduledPickupTime(request.getScheduledPickupTime())
                .build();

        Booking savedBooking = bookingRepository.save(newBooking);
        System.out.println("✓ Booking created with ID: " + savedBooking.getId() + " (no driver assigned yet)");

        // Notify all nearby drivers about this booking
        try {
            List<DriverLocationDTO> nearbyDrivers = locationService.getNearbyDrivers(
                    Double.parseDouble(pickupLat),
                    Double.parseDouble(pickupLon),
                    10.0
            );

            if (nearbyDrivers.isEmpty()) {
                throw new IllegalArgumentException("No available drivers nearby");
            }

            System.out.println("Notifying " + nearbyDrivers.size() + " nearby drivers about booking #" + savedBooking.getId());

            grpcClient.notifyDriverForNewRide(
                    nearbyDrivers.stream()
                            .map(DriverLocationDTO::getDriverId)
                            .collect(Collectors.toList()),
                    pickupLat,
                    pickupLon,
                    savedBooking.getId().intValue()
            );
        } catch (Exception e) {
            System.err.println("Warning: Failed to notify drivers - " + e.getMessage());
        }

        return bookingMapper.toResponse(savedBooking);
    }

    @Override
    public BookingResponse update(Long id, BookingRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        Passenger passenger = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found with id: " + request.getPassengerId()));

        Driver driver = null;
        if (request.getDriverId() != null) {
            driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + request.getDriverId()));
        }

        // Handle driver availability when updating
        Driver previousDriver = booking.getDriver();
        if (previousDriver != null && !previousDriver.equals(driver)) {
            previousDriver.setIsAvailable(true);
            driverRepository.save(previousDriver);
        }

        if (driver != null && !driver.equals(previousDriver)) {
            if (!driver.getIsAvailable()) {
                throw new IllegalArgumentException("Driver with id " + request.getDriverId() + " is not available");
            }
            driver.setIsAvailable(false);
            driverRepository.save(driver);
        }

        bookingMapper.updateEntity(booking, request, passenger, driver);
        Booking updatedBooking = bookingRepository.save(booking);
        return bookingMapper.toResponse(updatedBooking);
    }

    @Override
    public BookingResponse updateStatus(Long id, Booking.BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        booking.setStatus(status);

        // Handle status-specific logic
        if (status == Booking.BookingStatus.IN_PROGRESS && booking.getActualPickupTime() == null) {
            booking.setActualPickupTime(LocalDateTime.now());
        } else if (status == Booking.BookingStatus.COMPLETED) {
            booking.setCompletedAt(LocalDateTime.now());
            // Release driver
            if (booking.getDriver() != null) {
                Driver driver = booking.getDriver();
                driver.setIsAvailable(true);
                driverRepository.save(driver);
            }
        } else if (status == Booking.BookingStatus.CANCELLED) {
            // Release driver
            if (booking.getDriver() != null) {
                Driver driver = booking.getDriver();
                driver.setIsAvailable(true);
                driverRepository.save(driver);
            }
        }

        Booking updatedBooking = bookingRepository.save(booking);
        return bookingMapper.toResponse(updatedBooking);
    }

    @Override
    public Boolean acceptRide(Long id, Long driverId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

         //Only one driver can accept
        if (booking.getDriver() != null) {
            System.out.println("Booking #" + id + " already accepted by Driver #" + booking.getDriver().getId());
            return false;
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + driverId));

        if (!driver.getIsAvailable()) {
            System.out.println("Driver #" + driverId + " is not available");
            return false;
        }

        // NOW mark driver as unavailable (only when accepting)
        driver.setIsAvailable(false);
        driverRepository.save(driver);

        booking.setDriver(driver);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        System.out.println("Driver #" + driverId + " accepted Booking #" + id);
        System.out.println("Driver marked as unavailable");

        return true;
    }

    @Override
    public void deleteById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        // Release driver if assigned
        if (booking.getDriver() != null) {
            Driver driver = booking.getDriver();
            driver.setIsAvailable(true);
            driverRepository.save(driver);
            System.out.println("✓ Driver #" + driver.getId() + " released from Booking #" + id);
        }

        bookingRepository.deleteById(id);
    }
}
