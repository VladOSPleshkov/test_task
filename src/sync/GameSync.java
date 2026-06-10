package sync;

import model.Coordinate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

//CSV формат одной строки хода:
//shooterIndex,x,y,RESULT,aliveShips
//aliveShips - количество живых кораблей у того, в кого стреляли
//Записывает строку тот игрок, в чьё поле попали (он знает свои корабли)

public class GameSync {

    private static final Path GAME_EXISTS = Paths.get("game_exists.txt");
    private static final Path TURN_FILE = Paths.get("game_turn.txt");
    private static final Path MOVE_FILE = Paths.get("game_move.csv");
    private static final Path READY_0 = Paths.get("player0_ready.txt");
    private static final Path READY_1 = Paths.get("player1_ready.txt");

    public void reset() {
        deleteFiles(GAME_EXISTS, TURN_FILE, MOVE_FILE, READY_0, READY_1);
        write(GAME_EXISTS, "1");
    }

    public boolean gameExists() {
        return Files.exists(GAME_EXISTS);
    }

    public void signalReady(int playerIndex, String playerName) {
        write(playerIndex == 0 ? READY_0 : READY_1, playerName);
    }

    public String waitForOpponentReady(int myIndex, long timeoutMs) {
        Path opponentFile = myIndex == 0 ? READY_1 : READY_0;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (Files.exists(opponentFile)) {
                try {
                    String name = Files.readString(opponentFile).trim();
                    if (!name.isEmpty()) return name;
                } catch (IOException e) {
                    System.err.println("Ошибка чтения готовности: " + e.getMessage());
                }
            }
            sleep(500);
        }
        return null;
    }

    public void writeTurn(int playerIndex) {
        write(TURN_FILE, String.valueOf(playerIndex));
    }

    public int readTurn() {
        try {
            if (!Files.exists(TURN_FILE)) return 0;
            return Integer.parseInt(Files.readString(TURN_FILE).trim());
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    // Записывает стрелявший игрок: координаты и маркер "ожидание результата"
    public void writeShot(int shooterIndex, Coordinate coord) {
        write(MOVE_FILE, shooterIndex + "," + coord.x + "," + coord.y + ",PENDING,?");
    }

    // Записывает результат тот, в кого стреляли: он знает свои корабли
    public void writeResult(int shooterIndex, Coordinate coord, String result, int aliveShips) {
        write(MOVE_FILE, shooterIndex + "," + coord.x + "," + coord.y
                        + "," + result + "," + aliveShips);
    }

    //ждем появления выстрела противника (статус PENDING)
    public String[] waitForIncomingShot(int myIndex, long timeoutMs) {
        long start = System.currentTimeMillis();
        String lastSeen = readRaw();

        while (System.currentTimeMillis() - start < timeoutMs) {
            String current = readRaw();
            if (current != null && !current.equals(lastSeen)) {
                String[] parts = current.split(",");
                // Выстрел от противника, ещё не обработанный
                if (parts.length == 5
                        && Integer.parseInt(parts[0]) != myIndex
                        && "PENDING".equals(parts[3])) {
                    return parts;
                }
                lastSeen = current;
            }
            sleep(300);
        }
        return null;
    }

    // ждем результата своего выстрела (статус не PENDING и не пустой)
    public String[] waitForShotResult(int myIndex, long timeoutMs) {
        long start = System.currentTimeMillis();
        String lastSeen = readRaw();

        while (System.currentTimeMillis() - start < timeoutMs) {
            String current = readRaw();
            if (current != null && !current.equals(lastSeen)) {
                String[] parts = current.split(",");
                // Результат нашего выстрела — записан противником
                if (parts.length == 5
                        && Integer.parseInt(parts[0]) == myIndex
                        && !"PENDING".equals(parts[3])) {
                    return parts;
                }
                lastSeen = current;
            }
            sleep(300);
        }
        return null;
    }

    public void writeMove(int playerIndex, Coordinate coord, String result) {
        write(MOVE_FILE, playerIndex + "," + coord.x + "," + coord.y + "," + result + ",?");
    }

    public String[] readMove() {
        try {
            if (!Files.exists(MOVE_FILE)) return null;
            String line = Files.readString(MOVE_FILE).trim();
            return line.isEmpty() ? null : line.split(",");
        } catch (IOException e) {
            return null;
        }
    }

    public String[] waitForMove(int myPlayerIndex, long timeoutMs) {
        return waitForShotResult(myPlayerIndex, timeoutMs);
    }

    public void deleteQuietly() {
        deleteFiles(GAME_EXISTS, TURN_FILE, MOVE_FILE, READY_0, READY_1);
    }

    private String readRaw() {
        try {
            if (!Files.exists(MOVE_FILE)) {
                return null;
            }
            return Files.readString(MOVE_FILE).trim();
        } catch (IOException e) {
            return null;
        }
    }

    private void write(Path path, String content) {
        try {
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Ошибка записи " + path + ": " + e.getMessage());
        }
    }

    private void deleteFiles(Path... paths) {
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                System.err.println("Ошибка удаления " + path + ": " + e.getMessage());
            }
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
