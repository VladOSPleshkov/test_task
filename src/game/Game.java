package game;

import model.Coordinate;
import model.Field;
import sync.GameSync;

import java.util.Scanner;

public class Game {
    private final Field myField;
    private final Field opponentTracking;
    private final Scanner scanner;
    private final String name;
    private final boolean isTwoPlayer;
    private final int playerIndex;
    private final GameSync sync;
    private final BotAi botAI;
    private int opponentAliveShips;

    // конструктор для одиночной игры
    public Game(Field myField, Field opponentField, String name, Scanner scanner) {
        this.myField = myField;
        this.opponentTracking = opponentField;
        this.scanner = scanner;
        this.name = name;
        this.isTwoPlayer = false;
        this.playerIndex = 0;
        this.sync = null;
        this.botAI = new BotAi(Field.SIZE);
        this.opponentAliveShips = 0;
    }

    // конструктор для игры вдвоём
    public Game(Field myField, Field opponentTracking, String name, Scanner scanner,
                int playerIndex, GameSync sync, int totalOpponentShips) {
        this.myField = myField;
        this.opponentTracking = opponentTracking;
        this.scanner = scanner;
        this.name = name;
        this.isTwoPlayer = true;
        this.playerIndex = playerIndex;
        this.sync = sync;
        this.botAI = null;
        this.opponentAliveShips = totalOpponentShips;
    }

    public void start() {
        if (isTwoPlayer) {
            runTwoPlayerGame();
        } else {
            runSinglePlayerGame();
        }
    }

    private void runSinglePlayerGame() {
        //первый ход всегда у игрока
        boolean myTurn = true;

        while (myField.hasAliveShips() && opponentTracking.hasAliveShips()) {
            clearConsole();
            printBothFields();

            if (myTurn) {
                System.out.println(name + " ваш ход. Введите координаты для выстрела.");
                Coordinate shot = readShot(opponentTracking);
                ShotResult result = opponentTracking.receiveShot(shot);
                printShotResult(result);

                if (result == ShotResult.ALREADY_SHOT) {
                    continue;
                }
                if (result == ShotResult.MISS) {
                    myTurn = false;
                }

            } else {
                System.out.println("Ход противника (бот)...");
                Coordinate shot = botAI.nextShot();
                ShotResult result = myField.receiveShot(shot);
                botAI.handleResult(shot, result);
                System.out.println("Бот выстрелил: " + (char)('A' + shot.x) + (shot.y + 1) + " — " + resultText(result));

                if (result != ShotResult.MISS) {
                } else {
                    myTurn = true;
                }
                pause(1000);
            }
        }

        clearConsole();
        printBothFields();
        if (opponentTracking.hasAliveShips()) {
            System.out.println("Бот победил");
        } else {
            System.out.println(name + ", вы победили!");
        }
    }

    private void runTwoPlayerGame() {
        if (playerIndex == 0) sync.writeTurn(0);

        boolean gameOver = false;

        while (!gameOver) {
            clearConsole();
            printBothFields();

            int currentTurn = sync.readTurn();

            if (currentTurn == playerIndex) {
                gameOver = doMyTurn();
            } else {
                gameOver = handleOpponentTurn();
            }
        }

        clearConsole();
        printBothFields();

    }
    private boolean doMyTurn() {
        System.out.println(name + "Ваш ход:");
        Coordinate shot = readShot(opponentTracking);

        // Записываем выстрел со статусом PENDING — противник его прочитает
        sync.writeShot(playerIndex, shot);
        System.out.println("Выстрел отправлен. Ожидание результата...");

        String[] result = sync.waitForShotResult(playerIndex, 5 * 60 * 1000);
        if (result == null) {
            System.out.println("Противник не ответил. Завершение.");
            return true;
        }

        ShotResult shotResult = ShotResult.valueOf(result[3]);
        int opponentAlive = Integer.parseInt(result[4]);
        this.opponentAliveShips = opponentAlive;

        //отмечаем выстрел на нашей карте отслеживания
        opponentTracking.setCell(shot.x, shot.y,
                shotResult == ShotResult.MISS ? model.CellState.MISS : model.CellState.HIT);

        printShotResult(shotResult);

        if (opponentAlive == 0) {
            System.out.println("Вы победили, " + name + "!");
            sync.deleteQuietly();
            return true;
        }

        if (shotResult == ShotResult.MISS) {
            sync.writeTurn(1 - playerIndex);
        }

        pause(1000);
        return false;
    }

    //обрабатываем ход противника: читаем выстрел, считаем результат, записываем ответ
    private boolean handleOpponentTurn() {
        System.out.println("Ожидание хода противника...");

        String[] incoming = sync.waitForIncomingShot(playerIndex, 10 * 60 * 1000);
        if (incoming == null) {
            System.out.println("Противник не ответил. Завершение.");
            return true;
        }

        Coordinate shot = new Coordinate(
                Integer.parseInt(incoming[1]),
                Integer.parseInt(incoming[2]));

        ShotResult result = myField.receiveShot(shot);

        //сообщаем противнику результат и сколько наших кораблей осталось
        int myAlive = countAliveShips();
        sync.writeResult(1 - playerIndex, shot, result.name(), myAlive);

        System.out.println("Противник выстрелил: "
                + coordToString(shot) + " — " + resultText(result));

        if (myAlive == 0) {
            System.out.println("Вы проиграли.");
            sync.deleteQuietly();
            return true;
        }

        // Если противник промазал - теперь наш ход
        if (result == ShotResult.MISS) {
            sync.writeTurn(playerIndex);
        }

        pause(1000);
        return false;
    }

    private int countAliveShips() {
        int count = 0;
        for (int y = 0; y < Field.SIZE; y++) {
            for (int x = 0; x < Field.SIZE; x++) {
                if (myField.getCell(x, y) == model.CellState.SHIP) count++;
            }
        }
        return count;
    }
    private String coordToString(Coordinate c) {
        return String.valueOf((char)('A' + c.x)) + (c.y + 1);
    }

    private void printBothFields() {
        System.out.println("=Ваше поле=");
        myField.print(false);
        System.out.println("=Поле противника=");
        opponentTracking.print(true);
    }

    private Coordinate readShot(Field target) {
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

            if (!opponentTracking.inBounds(x, y)) {
                System.out.println("Координаты вне поля.");
                continue;
            }
            model.CellState state = opponentTracking.getCell(x, y);
            if (state == model.CellState.HIT || state == model.CellState.MISS) {
                System.out.println("Вы уже попали в эту клетку.");
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