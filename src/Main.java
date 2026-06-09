import game.Game;
import model.Field;
import sync.GameSync;

import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Морской бой");
        System.out.println("Введите ваше имя:");
        String name = scanner.nextLine();
        System.out.println("1 - Одиночная игра (против бота)");
        System.out.println("2 - Игра вдвоём (через файл синхронизации)");
        System.out.print("Выберите режим: ");

        int mode = readInt();

        if (mode == 1) {
            startSinglePlayer();
        } else if (mode == 2) {
            startTwoPlayer();
        } else {
            System.out.println("Неверный выбор.");
        }
    }

    private static void startSinglePlayer() {
        Field myField = new Field();
        Field botField = new Field();
        System.out.println("\nРасстановка кораблей");

        myField = buildField();
        //placeShips(myField);

        placeShipsRandomly(botField);

        new Game(myField, botField, scanner).start();
    }

    private static void choosePlacementMode(Field field) {
        System.out.println("Как вы хотите расставить свои корабли?");
        System.out.println("1 - Вручную");
        System.out.println("2 - Автоматически");
        System.out.print("Ваш выбор: ");
        int choice = readInt();

        if (choice == 1) {
            placeShips(field);
        } else {
            placeShipsRandomly(field);
            System.out.println("Автоматическая расстановка выполнена:");
            field.print(false);
            pause(2000);
        }
    }

    private static void startTwoPlayer() {
        System.out.print("Введите ваш номер игрока (0 или 1): ");
        int playerIndex = readInt();
        if (playerIndex != 0 && playerIndex != 1) {
            System.out.println("Неверный номер игрока.");
            return;
        }

        Field myField = new Field();
        //поле противника - только для отслеживания выстрелов
        Field opponentTrackingField = new Field();

        System.out.println("\nРасставьте свои корабли.");
        placeShips(myField);

        System.out.println("Ожидайте, пока второй игрок расставит корабли...");
        // Небольшая пауза, чтобы игрок успел прочитать сообщение
        pause(2000);

        GameSync sync = new GameSync();
        if (playerIndex == 0) sync.clearSync();

        new Game(myField, opponentTrackingField, scanner, playerIndex, sync).start();
    }

    private static void placeShips(Field field) {

        int[] fleet = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};

        System.out.println("Вам нужно разместить следующие корабли:");
        System.out.println("1 корабль размером 4, 2 корабля по 3, 3 корабля по 2, 4 корабля по 1");

        for (int shipSize : fleet) {
            boolean placed = false;
            while (!placed) {
                field.print(false);
                System.out.println("Разместите корабль размером " + shipSize + ".");

                System.out.print("Столбец начала (A-J): ");
                String colInput = scanner.next().trim().toUpperCase();

                System.out.print("Строка начала (1-" + Field.SIZE + "): ");
                String rowInput = scanner.next().trim();

                System.out.print("Направление (H - горизонтально, V - вертикально): ");
                String dirInput = scanner.next().trim().toUpperCase();

                //валидация столбца
                if (colInput.length() != 1 || colInput.charAt(0) < 'A' ||
                        colInput.charAt(0) > 'A' + Field.SIZE - 1) {
                    System.out.println("Неверный столбец. Введите букву от A до " +
                            (char)('A' + Field.SIZE - 1) + ".");
                    continue;
                }

                //валидация строки
                int x, y;
                try {
                    y = Integer.parseInt(rowInput) - 1;
                } catch (NumberFormatException e) {
                    System.out.println("Неверная строка. Введите число от 1 до " + Field.SIZE + ".");
                    continue;
                }
                x = colInput.charAt(0) - 'A';

                if (!field.inBounds(x, y)) {
                    System.out.println("Начальная позиция вне поля.");
                    continue;
                }

                //валидация направления
                if (!dirInput.equals("H") && !dirInput.equals("V")) {
                    System.out.println("Неверное направление. Введите H или V.");
                    continue;
                }
                boolean horizontal = dirInput.equals("H");

                //можно ли разместить корабль
                if (!field.canPlaceShip(x, y, shipSize, horizontal)) {
                    System.out.println("Невозможно разместить корабль в этой позиции: " +
                            "корабль выходит за границы поля или пересекается с другим кораблём " +
                            "(включая зону вокруг кораблей).");
                    continue;
                }

                field.addShip(x, y, shipSize, horizontal);
                placed = true;
            }
        }

        System.out.println("Все корабли расставлены.");
        field.print(false);
        pause(1500);
    }

    private static Field buildField() {
        System.out.println("Как расставить корабли?");
        System.out.println("1 - Вручную");
        System.out.println("2 - Автоматически");
        System.out.print("Ваш выбор: ");
        int choice = readInt();

        Field field = new Field();

        if (choice == 1) {
            placeShips(field);
        } else {
            placeShipsRandomly(field);
            System.out.println("Корабли расставлены автоматически:");
            field.print(false);
            pause(2000);
        }

        return field;
    }


    //случайная расстановка
    private static void placeShipsRandomly(Field field) {
        int[] fleet = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};
        var random = new java.util.Random();

        for (int shipSize : fleet) {
            boolean placed = false;
            while (!placed) {
                int x = random.nextInt(Field.SIZE);
                int y = random.nextInt(Field.SIZE);
                boolean horizontal = random.nextBoolean();
                if (field.canPlaceShip(x, y, shipSize, horizontal)) {
                    field.addShip(x, y, shipSize, horizontal);
                    placed = true;
                }
            }
        }
    }

    private static int readInt() {
        while (true) {
            try {
                return Integer.parseInt(scanner.next().trim());
            } catch (NumberFormatException e) {
                System.out.print("Введите целое число: ");
            }
        }
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}