package com.example.Uber.service.impl;

import com.example.RideMateXSocket.RideAcceptanceRequest;
import com.example.RideMateXSocket.RideAcceptanceResponse;
import com.example.RideMateXSocket.RideServiceGrpc;
import com.example.Uber.service.BookingService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RideServiceImpl extends RideServiceGrpc.RideServiceImplBase {
    private final BookingService bookingService;

    @Override
    public void acceptRide(RideAcceptanceRequest request, StreamObserver<RideAcceptanceResponse> responseObserver) {
        Boolean success = bookingService.acceptRide(Long.parseLong("" + request.getBookingId()),
                Long.parseLong("" + request.getDriverId()));

        RideAcceptanceResponse response = RideAcceptanceResponse.newBuilder()
                .setSuccess(success)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
