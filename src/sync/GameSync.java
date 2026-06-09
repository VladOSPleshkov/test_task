package sync;

import model.Coordinate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

// Синхронизация хода между двумя процессами через общий файл.
// Формат файла: одна строка — "playerIndex,x,y,result"
// playerIndex: 0 или 1 (кто сделал ход)

public class GameSync {
    private static final Path SYNC_FILE = Paths.get("game_sync.txt");
    private static final Path TURN_FILE = Paths.get("game_turn.txt");

    // Записываем результат хода
    public void writeMove(int playerIndex, Coordinate coord, String result) {
        try {
            String line = playerIndex + "," + coord.x + "," + coord.y + "," + result;
            Files.writeString(SYNC_FILE, line, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Ошибка записи хода: " + e.getMessage());
        }
    }

    // Читаем последний ход
    public String[] readMove() {
        try {
            if (!Files.exists(SYNC_FILE)) return null;
            String line = Files.readString(SYNC_FILE).trim();
            if (line.isEmpty()) return null;
            return line.split(",");
        } catch (IOException e) {
            System.err.println("Ошибка чтения хода: " + e.getMessage());
            return null;
        }
    }

    // Записываем, чей сейчас ход (0 или 1)
    public void writeTurn(int playerIndex) {
        try {
            Files.writeString(TURN_FILE, String.valueOf(playerIndex),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Ошибка записи очерёдности: " + e.getMessage());
        }
    }

    // Читаем, чей сейчас ход
    public int readTurn() {
        try {
            if (!Files.exists(TURN_FILE)) return 0;
            return Integer.parseInt(Files.readString(TURN_FILE).trim());
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    public void clearSync() {
        try {
            Files.deleteIfExists(SYNC_FILE);
            Files.deleteIfExists(TURN_FILE);
        } catch (IOException e) {
            System.err.println("Ошибка очистки файлов синхронизации: " + e.getMessage());
        }
    }

    // Ожидание обновления файла хода (ждём, пока ход сделает другой игрок)
    public String[] waitForMove(int myPlayerIndex, long timeoutMs) {
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeoutMs) {
            String[] move = readMove();
            if (move != null && Integer.parseInt(move[0]) != myPlayerIndex) {
                return move;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }
}
