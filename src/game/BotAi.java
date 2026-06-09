package game;

import model.Coordinate;

import java.util.HashSet;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.LinkedList;

public class BotAi {
    private final int size;
    private final Set<Coordinate> used = new HashSet<>();
    private final Queue<Coordinate> priorityTargets = new LinkedList<>();
    private final Random random = new Random();

    public BotAi(int size) {
        this.size = size;
    }

    public Coordinate nextShot() {
        // ход на добивание корабля
        while (!priorityTargets.isEmpty()) {
            Coordinate c = priorityTargets.poll();
            if (!used.contains(c) && inBounds(c)) {
                used.add(c);
                return c;
            }
        }
        Coordinate shot;
        do {
            shot = new Coordinate(random.nextInt(size), random.nextInt(size));
        } while (used.contains(shot));

        used.add(shot);
        return shot;
    }

    public void handleResult(Coordinate shot, ShotResult result) {
        if (result == ShotResult.HIT) {
            addNeighbours(shot);
        } else if (result == ShotResult.SUNK) {
            priorityTargets.clear();
        }
    }

    private void addNeighbours(Coordinate c) {
        priorityTargets.add(new Coordinate(c.x + 1, c.y));
        priorityTargets.add(new Coordinate(c.x - 1, c.y));
        priorityTargets.add(new Coordinate(c.x, c.y + 1));
        priorityTargets.add(new Coordinate(c.x, c.y - 1));
    }

    private boolean inBounds(Coordinate c) {
        return c.x >= 0 && c.y >= 0 && c.x < size && c.y < size;
    }
}