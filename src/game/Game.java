package game;

import model.Coordinate;
import model.Field;
import sync.GameSync;

import java.util.Scanner;

public class Game {
    private final Field myField;
    private final Field opponentField;
    private final Scanner scanner;
    private final boolean isTwoPlayer;
    private final int playerIndex;
    private final GameSync sync;
    private final BotAi botAI;

    // конструктор для одиночной игры
    public Game(Field myField, Field opponentField, Scanner scanner) {
        this.myField = myField;
        this.opponentField = opponentField;
        this.scanner = scanner;
        this.isTwoPlayer = false;
        this.playerIndex = 0;
        this.sync = null;
        this.botAI = new BotAi(Field.SIZE);
    }

    // конструктор для игры вдвоём
    public Game(Field myField, Field opponentField, Scanner scanner,
                int playerIndex, GameSync sync) {
        this.myField = myField;
        this.opponentField = opponentField;
        this.scanner = scanner;
        this.isTwoPlayer = true;
        this.playerIndex = playerIndex;
        this.sync = sync;
        this.botAI = null;
    }

    public void start() {
        if (isTwoPlayer) {
            runTwoPlayerGame();
        } else {
            runSinglePlayerGame();
        }
    }

    private void runSinglePlayerGame() {
        // Первый ход всегда у игрока
        boolean myTurn = true;

        while (myField.hasAliveShips() && opponentField.hasAliveShips()) {
            clearConsole();
            printBothFields();

            if (myTurn) {
                System.out.println("Ваш ход. Введите координаты для выстрела.");
                Coordinate shot = readShot();
                ShotResult result = opponentField.receiveShot(shot);
                printShotResult(result);

                if (result == ShotResult.ALREADY_SHOT) continue;
                if (result == ShotResult.MISS) myTurn = false;

            } else {
                System.out.println("Ход противника (бот)...");
                Coordinate shot = botAI.nextShot();
                ShotResult result = myField.receiveShot(shot);
                botAI.handleResult(shot, result);
                System.out.println("Бот выстрелил: " +
                        (char)('A' + shot.x) + (shot.y + 1) + " — " + resultText(result));

                if (result != ShotResult.MISS) {
                    // Бот продолжает ход при попадании
                } else {
                    myTurn = true;
                }
                pause(1000);
            }
        }

        clearConsole();
        printBothFields();
        if (opponentField.hasAliveShips()) {
            System.out.println("Игрокпобедил");
        } else {
            System.out.println("Вы победили!");
        }
    }

    private void runTwoPlayerGame() {
        // Игрок 0 ходит первым
        sync.writeTurn(0);

        while (myField.hasAliveShips() && opponentField.hasAliveShips()) {
            clearConsole();
            printBothFields();

            int currentTurn = sync.readTurn();

            if (currentTurn == playerIndex) {
                System.out.println("Ваш ход. Введите координаты для выстрела:");
                Coordinate shot = readShot();
                ShotResult result = opponentField.receiveShot(shot);
                printShotResult(result);

                if (result == ShotResult.ALREADY_SHOT) continue;

                // Записываем ход и передаём очередь, если промах
                sync.writeMove(playerIndex, shot, result.name());
                if (result == ShotResult.MISS) {
                    sync.writeTurn(1 - playerIndex);
                }

                if (!opponentField.hasAliveShips()) break;

            } else {
                System.out.println("Ожидание хода противника...");
                // Таймаут 5 минут
                String[] move = sync.waitForMove(playerIndex, 5 * 60 * 1000);

                if (move == null) {
                    System.out.println("Противник не ответил. Завершение игры.");
                    return;
                }

                Coordinate shot = new Coordinate(Integer.parseInt(move[1]), Integer.parseInt(move[2]));
                ShotResult result = myField.receiveShot(shot);

                System.out.println("Противник выстрелил: " +
                        (char)('A' + shot.x) + (shot.y + 1) + " — " + resultText(result));

                if (result == ShotResult.MISS) {
                    sync.writeTurn(playerIndex);
                }

                pause(1000);
            }
        }
        clearConsole();
        printBothFields();
        if (!opponentField.hasAliveShips()) {
            System.out.println("Вы победили");
        } else {
            System.out.println("Вы проиграли");
        }

        if (sync != null) sync.clearSync();
    }

    private void printBothFields() {
        System.out.println("=Ваше поле=");
        myField.print(false);
        System.out.println("=Поле противника=");
        opponentField.print(true);
    }

    private Coordinate readShot() {
        while (true) {
            System.out.print("Столбец (A-P): ");
            String colInput = scanner.next().trim().toUpperCase();
            System.out.print("Строка (1-" + Field.SIZE + "): ");
            String rowInput = scanner.next().trim();

            if (colInput.length() != 1 || colInput.charAt(0) < 'A' ||
                    colInput.charAt(0) > 'A' + Field.SIZE - 1) {
                System.out.println("Неверный столбец. Введите букву от A до " +
                        (char)('A' + Field.SIZE - 1) + ".");
                continue;
            }

            int x = colInput.charAt(0) - 'A';
            int y;
            try {
                y = Integer.parseInt(rowInput) - 1;
            } catch (NumberFormatException e) {
                System.out.println("Неверная строка. Введите число от 1 до " + Field.SIZE + ".");
                continue;
            }

            if (!opponentField.inBounds(x, y)) {
                System.out.println("Координаты вне поля.");
                continue;
            }

            return new Coordinate(x, y);
        }
    }

    private void printShotResult(ShotResult result) {
        switch (result) {
            case HIT -> System.out.println("Попадание!");
            case SUNK -> System.out.println("Корабль потоплен!");
            case MISS -> System.out.println("Мимо.");
            case ALREADY_SHOT -> System.out.println("Вы уже стреляли сюда. Повторите.");
        }
    }

    private String resultText(ShotResult result) {
        return switch (result) {
            case HIT -> "попадание";
            case SUNK -> "корабль потоплен";
            case MISS -> "мимо";
            case ALREADY_SHOT -> "повтор";
        };
    }

    private void clearConsole() {
        //ansi-escape для очистки консоли
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}