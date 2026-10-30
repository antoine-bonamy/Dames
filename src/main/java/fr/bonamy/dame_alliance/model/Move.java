package fr.bonamy.dame_alliance.model;

import fr.bonamy.dame_alliance.util.Point;

import java.util.List;

/**
 * Coup complet : chemin parcouru (case de départ incluse) et positions des pièces prises.
 */
public record Move(List<Point> path, List<Point> captured) {

    public Move {
        path = List.copyOf(path);
        captured = List.copyOf(captured);
    }

    public Point from() {
        return path.getFirst();
    }

    public Point to() {
        return path.getLast();
    }

    public boolean isCapture() {
        return !captured.isEmpty();
    }
}
