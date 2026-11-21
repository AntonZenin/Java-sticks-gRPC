package ru.sticks.client;

import ru.sticks.common.GameState;
import ru.sticks.common.GameStateMapper;
import ru.sticks.grpc.GameStateResponse;
import ru.sticks.grpc.GetStateRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import ru.sticks.grpc.JoinRequest;
import ru.sticks.grpc.JoinResponse;
import ru.sticks.grpc.StickGameServiceGrpc;
import ru.sticks.grpc.MoveRequest;
import java.util.concurrent.atomic.AtomicBoolean;

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

        AtomicBoolean running = new AtomicBoolean(true);
        Object consoleLock = new Object();

        try {
            StickGameServiceGrpc.StickGameServiceBlockingStub stub = StickGameServiceGrpc.newBlockingStub(channel);

            JoinRequest request = JoinRequest.newBuilder().setName(name).build();

            JoinResponse response = stub.withDeadlineAfter(5, TimeUnit.SECONDS).join(request);

            int playerId = response.getPlayerId();
            if (playerId == -1) {
                System.out.println("В игре уже два игрока");
                return;
            }
            System.out.println("Вы игрок номер " + playerId);

            System.out.println("Формат хода: h строка столбец или v строка столбец");

            Thread inputThread = new Thread(
                    () -> readMoves(in, stub, playerId, running, consoleLock),
                    "move-input");
            inputThread.setDaemon(true);
            inputThread.start();


            GetStateRequest stateRequest = GetStateRequest.newBuilder().build();

            int lastVersion = -1;

            while (running.get()) {
                GameStateResponse stateResponse = stub.withDeadlineAfter(5, TimeUnit.SECONDS).getState(stateRequest);

                if (stateResponse.getVersion() != lastVersion) {
                    GameState state = GameStateMapper.fromResponse(stateResponse);
                    synchronized (consoleLock) {
                        state.print();
                    }
                    lastVersion = stateResponse.getVersion();
                }

                if (stateResponse.getGameOver()) {
                    break;
                }
                Thread.sleep(200);
            }
        } catch (StatusRuntimeException e) {
            System.out.println("Ошибка gRPC: " + e.getStatus());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            running.set(false);
            channel.shutdownNow();
        }
    }

    private static void readMoves(Scanner in, StickGameServiceGrpc.StickGameServiceBlockingStub stub, int playerId,
                                  AtomicBoolean running, Object consoleLock) {
        try {
            while(running.get() && in.hasNextLine()) {
                String line = in.nextLine().strip();
                if (!running.get()) {
                    break;
                }

                String[] parts = line.split("\\s+");
                if (parts.length != 3 || parts[0].length() != 1) {
                    synchronized (consoleLock) {
                        System.out.println("Введите ход в формате h 0 0 или v 0 0");
                    }
                    continue;
                }
                try {
                    MoveRequest request = MoveRequest.newBuilder().setPlayerId(playerId).setType(parts[0])
                            .setRow(Integer.parseInt(parts[1]))
                            .setCol(Integer.parseInt(parts[2]))
                            .build();

                    String result = stub.withDeadlineAfter(5, TimeUnit.SECONDS).makeMove(request).getResult();

                    if (!"OK".equals(result)) {
                        synchronized (consoleLock) {
                            System.out.println("Ошибка: " + result);
                        }
                    }
                } catch (NumberFormatException e) {
                    synchronized (consoleLock) {
                        System.out.println("Строка и столбец должны быть целыми числами.");
                    }
                }
            }
        } catch (StatusRuntimeException e) {
            synchronized (consoleLock) {
                if (running.get()) {
                    System.out.println("Ошибка gRPC при отправке хода: " + e.getStatus());
                }
            }
        } finally {
            running.set(false);
        }
    }
}
