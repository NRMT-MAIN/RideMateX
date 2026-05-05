package com.example.Uber.config;

import com.example.Uber.service.impl.RideServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class GrpcServerConfig {
    @Value("${grpc.server.port:9090}")
    private int grpcServerPort;

    private Server grpcServer ;

    private final RideServiceImpl rideServiceImpl;


    @PostConstruct
    public void startGrpcServer() throws IOException {
        grpcServer = ServerBuilder.forPort(grpcServerPort)
                .addService(rideServiceImpl)
                .build()
                .start();
        System.out.println("gRPC server started on port " + grpcServerPort);

        new Thread(() -> {
            try {
                if(grpcServer != null) {
                    grpcServer.awaitTermination();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gRPC server...");
            if (grpcServer != null) {
                grpcServer.shutdown();
            }
        }));
    }
}
