package model;

import game.ShotResult;

import java.util.ArrayList;
import java.util.List;

public class Field {
    public static final int SIZE = 16;
    private static final String COL_LABELS = "ABCDEFGHIJKLMNOP";

    private final CellState[][] grid;
    private final List<Ship> ships = new ArrayList<>();

    public Field() {
        grid = new CellState[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                grid[y][x] = CellState.EMPTY;
    }

    public CellState getCell(int x, int y) {
        return grid[y][x];
    }

    public void setCell(int x, int y, CellState state) {
        grid[y][x] = state;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE;
    }

    public ShotResult receiveShot(Coordinate shot) {
        CellState current = grid[shot.y][shot.x];
        if (current == CellState.HIT || current == CellState.MISS) {
            return ShotResult.ALREADY_SHOT;
        }

        for (Ship ship : ships) {
            if (ship.contains(shot)) {
                ship.registerHit(shot);
                grid[shot.y][shot.x] = CellState.HIT;
                return ship.isSunk() ? ShotResult.SUNK : ShotResult.HIT;
            }
        }

        grid[shot.y][shot.x] = CellState.MISS;
        return ShotResult.MISS;
    }

    public boolean hasAliveShips() {
        for (Ship ship : ships)
            if (!ship.isSunk()) return true;
        return false;
    }

    // Проверяем, что клетки под корабль пусты и вокруг нет других кораблей
    public boolean canPlaceShip(int x, int y, int length, boolean horizontal) {
        for (int i = 0; i < length; i++) {
            int cx;
            int cy;
            if (horizontal) {
                cx = x + i;
                cy = y;
            } else {
                cx = x;
                cy = y + i;
            }

            if (!inBounds(cx, cy)) return false;

            // Проверяем клетку и все соседние клетки вокруг неё
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = cx + dx;
                    int ny = cy + dy;
                    if (inBounds(nx, ny) && grid[ny][nx] == CellState.SHIP) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void addShip(int x, int y, int length, boolean horizontal) {
        var positions = new java.util.HashSet<Coordinate>();
        for (int i = 0; i < length; i++) {
            int cx;
            int cy;
            if (horizontal) {
                cx = x + i;
                cy = y;
            } else {
                cx = x;
                cy = y + i;
            }
            positions.add(new Coordinate(cx, cy));
            grid[cy][cx] = CellState.SHIP;
        }
        ships.add(new Ship(positions));
    }

    public void print(boolean hideShips) {
        System.out.print("   ");
        for (int i = 0; i < SIZE; i++) {
            System.out.print(COL_LABELS.charAt(i) + " ");
        }
        System.out.println();
        for (int y = 0; y < SIZE; y++) {
            int rowNumber = y + 1;
            if (rowNumber < 10) {
                System.out.print(rowNumber + "  ");
            } else {
                System.out.print(rowNumber + " ");
            }

            for (int x = 0; x < SIZE; x++) {
                CellState state = grid[y][x];
                char symbol = '~';

                if (state == CellState.SHIP) {
                    if (hideShips) {
                        symbol = '~';
                    } else {
                        symbol = 'O';
                    }
                } else if (state == CellState.HIT) {
                    symbol = 'X';
                } else if (state == CellState.MISS) {
                    symbol = '*';
                }

                System.out.print(symbol + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}