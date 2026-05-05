package com.example.Uber.client;

import com.example.RideMateXSocket.RideNotificationRequest;
import com.example.RideMateXSocket.RideNotificationResponse;
import com.example.RideMateXSocket.RideNotificationServiceGrpc;
import io.grpc.ManagedChannel;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class GrpcClient {
    @Value("${grpc.server.port:9090}")
    private int grpcServerPort;

    @Value("${grpc.server.host:localhost}")
    private String grpcServerHost;


    private ManagedChannel channel;
    private RideNotificationServiceGrpc.RideNotificationServiceBlockingStub rideServiceBlockingStub;


    @PostConstruct
    public void init() {
        channel = io.grpc.ManagedChannelBuilder.forAddress(grpcServerHost, grpcServerPort)
                .usePlaintext()
                .build();

        rideServiceBlockingStub = RideNotificationServiceGrpc.newBlockingStub(channel);
    }

    public boolean notifyDriverForNewRide(List<Integer> driverIds, String pickupLatitude, String pickupLongitude, Integer bookingId) {
        RideNotificationRequest request = RideNotificationRequest.newBuilder()
                .setPickupLocationLatitude(pickupLatitude)
                .setPickupLocationLongitude(pickupLongitude)
                .setBookingId(bookingId)
                .addAllDriverIds(driverIds)
                .build();

        RideNotificationResponse response = rideServiceBlockingStub.notifyDriverForNewRide(request);
        return response.getSuccess();
    }

}
