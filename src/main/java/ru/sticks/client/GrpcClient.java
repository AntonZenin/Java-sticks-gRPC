package ru.sticks.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import ru.sticks.grpc.JoinRequest;
import ru.sticks.grpc.JoinResponse;
import ru.sticks.grpc.StickGameServiceGrpc;
import ru.sticks.server.StickGameEngine;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class GrpcClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 50051;

        Scanner in = new Scanner(System.in);
        System.out.print("Введите имя: ");
        String name = in.nextLine().strip();

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        try {
            StickGameServiceGrpc.StickGameServiceBlockingStub stub = StickGameServiceGrpc.newBlockingStub(channel);

            JoinRequest request = JoinRequest.newBuilder().setName(name).build();

            JoinResponse response = stub.withDeadlineAfter(5, TimeUnit.SECONDS).join(request);

            int playerId = response.getPlayerId();
            if (playerId == -1) {
                System.out.println("В игре уже два игрока");
            } else {
                System.out.println("Вы игрок номер " + playerId);
            }
        } catch (StatusRuntimeException e) {
            System.out.println("Не удалось подключиться к игре: " + e.getStatus());
        } finally {
            channel.shutdownNow();
        }
    }
}
