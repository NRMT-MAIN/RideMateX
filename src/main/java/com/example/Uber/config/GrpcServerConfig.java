package com.example.Uber.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

public class GrpcServerConfig {
    @Value("${grpc.server.port:9090}")
    private int grpcServerPort= 9090;

    private Server grpcServer ;

    @PostConstruct
    public void startGrpcServer() throws IOException {
        grpcServer = ServerBuilder.forPort(grpcServerPort)
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
