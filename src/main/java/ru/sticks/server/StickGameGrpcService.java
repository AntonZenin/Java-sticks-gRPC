package ru.sticks.server;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import ru.sticks.common.GameStateMapper;
import ru.sticks.grpc.GameStateResponse;
import ru.sticks.grpc.GetStateRequest;
import ru.sticks.grpc.JoinRequest;
import ru.sticks.grpc.JoinResponse;
import ru.sticks.grpc.MoveRequest;
import ru.sticks.grpc.MoveResponse;
import ru.sticks.grpc.StickGameServiceGrpc;

public class StickGameGrpcService extends StickGameServiceGrpc.StickGameServiceImplBase {

    private final StickGameEngine engine;

    public StickGameGrpcService(StickGameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void join(JoinRequest request, StreamObserver<JoinResponse> observer) {
        String name = request.getName().strip();
        if (name.isEmpty()) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription("Имя игрока не должно быть пустым")
                    .asRuntimeException());
            return;
        }
        int playerId = engine.join(name);
        JoinResponse response = JoinResponse.newBuilder().setPlayerId(playerId).build();

        observer.onNext(response);
        observer.onCompleted();
    }

    @Override
    public void getState(GetStateRequest request, StreamObserver<GameStateResponse> observer) {
        GameStateResponse response = GameStateMapper.toResponse(engine.getState());

        observer.onNext(response);
        observer.onCompleted();
    }

    @Override
    public void makeMove(MoveRequest request, StreamObserver<MoveResponse> observer) {
        String type = request.getType();
        String result;

        if (type.length() == 1) {
            result = engine.makeMove(request.getPlayerId(), type.charAt(0), request.getRow(), request.getCol());
        } else {
            result = "Тип палочки должен быть h или v";
        }
        MoveResponse response = MoveResponse.newBuilder().setResult(result).build();
        observer.onNext(response);
        observer.onCompleted();
    }
}
