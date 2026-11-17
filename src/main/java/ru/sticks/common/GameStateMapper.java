package ru.sticks.common;

import ru.sticks.grpc.GameStateResponse;

public class GameStateMapper {

    public static GameStateResponse toResponse(GameState state) {
        GameStateResponse.Builder response = GameStateResponse.newBuilder()
                .setSize(state.size)
                .setCurrentPlayer(state.currentPlayer)
                .setStarted(state.started)
                .setGameOver(state.gameOver)
                .setVersion(state.version)
                .setMessage(state.message);

        for (boolean[] row : state.hor) {
            for (boolean stick : row) {
                response.addHorizontal(stick);
            }
        }

        for (boolean[] row : state.ver) {
            for (boolean stick : row) {
                response.addVertical(stick);
            }
        }

        for (int[] row : state.owner) {
            for (int player : row) {
                response.addOwners(player);
            }
        }

        for (int player = 1; player <= 2; player++) {
            String name = state.names[player];
            response.addNames(name == null ? "" : name);
            response.addScores(state.score[player]);
        }

        return response.build();
    }

    public static GameState fromResponse(GameStateResponse response) {
        GameState state = new GameState(response.getSize());

        state.currentPlayer = response.getCurrentPlayer();
        state.started = response.getStarted();
        state.gameOver = response.getGameOver();
        state.version = response.getVersion();
        state.message = response.getMessage();

        int index = 0;
        for (boolean[] row : state.hor) {
            for (int col = 0; col < row.length; col++) {
                row[col] = response.getHorizontal(index++);
            }
        }

        index = 0;
        for (boolean[] row : state.ver) {
            for (int col = 0; col < row.length; col++) {
                row[col] = response.getVertical(index++);
            }
        }

        index = 0;
        for (int[] row : state.owner) {
            for (int col = 0; col < row.length; col++) {
                row[col] = response.getOwners(index++);
            }
        }

        for (int player = 1; player <= 2; player++) {
            String name = response.getNames(player - 1);
            state.names[player] = name.isEmpty() ? null : name;
            state.score[player] = response.getScores(player - 1);
        }

        return state;
    }
}