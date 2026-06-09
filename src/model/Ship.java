package model;

import java.util.HashSet;
import java.util.Set;

public class Ship {
    private final Set<Coordinate> positions;
    private final Set<Coordinate> hits = new HashSet<>();

    public Ship(Set<Coordinate> positions) {
        this.positions = new HashSet<>(positions);
    }

    public boolean contains(Coordinate c) {
        return positions.contains(c);
    }

    public void registerHit(Coordinate c) {
        hits.add(c);
    }

    public boolean isSunk() {
        return hits.containsAll(positions);
    }

    public Set<Coordinate> getPositions() {
        return positions;
    }
}