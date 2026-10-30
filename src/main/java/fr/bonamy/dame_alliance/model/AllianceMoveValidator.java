package fr.bonamy.dame_alliance.model;

import fr.bonamy.dame_alliance.util.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Règles des Dames Alliance : un pion capture dans les directions de sa croix (avant et arrière)
 * et se déplace dans les autres directions, sans jamais reculer.
 */
public class AllianceMoveValidator implements MoveValidator {

    private static final Point N = new Point(0, -1);
    private static final Point S = new Point(0, 1);
    private static final Point E = new Point(1, 0);
    private static final Point W = new Point(-1, 0);
    private static final Point NE = new Point(1, -1);
    private static final Point NW = new Point(-1, -1);
    private static final Point SE = new Point(1, 1);
    private static final Point SW = new Point(-1, 1);

    private static final Set<Point> ORTHOGONALS = Set.of(N, S, E, W);
    private static final Set<Point> DIAGONALS = Set.of(NE, NW, SE, SW);
    private static final Set<Point> ALL_DIRECTIONS = Set.of(N, S, E, W, NE, NW, SE, SW);

    @Override
    public Set<Move> getLegalMoves(PieceColor color, Map<Point, Piece> boardState) {
        Set<Move> captures = new HashSet<>();
        boardState.forEach((position, piece) -> {
            if (piece.getColor() == color) {
                collectCaptures(piece, List.of(position), List.of(), boardState, captures);
            }
        });
        if (!captures.isEmpty()) {
            int longest = captures.stream().mapToInt(move -> move.captured().size()).max().orElseThrow();
            return captures.stream()
                    .filter(move -> move.captured().size() == longest)
                    .collect(Collectors.toSet());
        }

        Set<Move> moves = new HashSet<>();
        boardState.forEach((position, piece) -> {
            if (piece.getColor() == color) {
                for (Point direction : moveDirections(piece)) {
                    Point target = position.add(direction);
                    if (isWithinBounds(target) && !boardState.containsKey(target)) {
                        moves.add(new Move(List.of(position, target), List.of()));
                    }
                }
            }
        });
        return moves;
    }

    @Override
    public boolean canBePromoted(Piece piece, Point to) {
        int lastRow = (piece.getColor() == PieceColor.WHITE)
                ? GameConfig.getBoardSize() - 1
                : 0;

        return piece.getType() != PieceType.KING && to.getB() == lastRow;
    }

    /**
     * Explore récursivement les rafles. Les pièces sautées restent sur le plateau jusqu'à la fin du coup :
     * elles bloquent l'atterrissage et ne peuvent pas être sautées deux fois.
     */
    private void collectCaptures(Piece piece, List<Point> path, List<Point> captured,
                                 Map<Point, Piece> boardState, Set<Move> result) {
        Point current = path.getLast();
        boolean extended = false;
        for (Point direction : captureDirections(piece)) {
            Point jumped = current.add(direction);
            Point landing = jumped.add(direction);
            if (canCapture(piece, boardState.get(jumped))
                    && !captured.contains(jumped)
                    && isFreeLanding(landing, path.getFirst(), boardState)) {
                extended = true;
                collectCaptures(piece, append(path, landing), append(captured, jumped), boardState, result);
            }
        }
        if (!extended && !captured.isEmpty()) {
            result.add(new Move(path, captured));
        }
    }

    private boolean canCapture(Piece attacker, Piece target) {
        return target != null
                && target.getColor() != attacker.getColor()
                && (target.getType() != PieceType.KING || attacker.getType() == PieceType.KING);
    }

    private boolean isFreeLanding(Point landing, Point origin, Map<Point, Piece> boardState) {
        return isWithinBounds(landing) && (!boardState.containsKey(landing) || landing.equals(origin));
    }

    private Set<Point> captureDirections(Piece piece) {
        return switch (piece.getType()) {
            case KING -> ALL_DIRECTIONS;
            case ORTHOGONAL -> ORTHOGONALS;
            case DIAGONAL -> DIAGONALS;
        };
    }

    private Set<Point> moveDirections(Piece piece) {
        Set<Point> directions = switch (piece.getType()) {
            case KING -> ALL_DIRECTIONS;
            case ORTHOGONAL -> DIAGONALS;
            case DIAGONAL -> ORTHOGONALS;
        };
        if (piece.getType() == PieceType.KING) {
            return directions;
        }
        int backward = -piece.getColor().forward();
        return directions.stream()
                .filter(direction -> direction.getB() != backward)
                .collect(Collectors.toSet());
    }

    private static List<Point> append(List<Point> list, Point point) {
        List<Point> copy = new ArrayList<>(list);
        copy.add(point);
        return copy;
    }

    private boolean isWithinBounds(Point position) {
        int size = GameConfig.getBoardSize();
        return position.getA() >= 0 && position.getA() < size && position.getB() >= 0 && position.getB() < size;
    }
}
