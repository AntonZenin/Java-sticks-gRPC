package ru.sticks.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class GrpcServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        int size = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 50051;

        StickGameEngine engine = new StickGameEngine(size);
        StickGameGrpcService service = new StickGameGrpcService(engine);

        Server server = ServerBuilder.forPort(port).addService(service).build();

        server.start();
        System.out.println("gRPC-сервер запущен. Поле " + size + "x" + size + ", порт " + server.getPort());

        server.awaitTermination();
    }
}
