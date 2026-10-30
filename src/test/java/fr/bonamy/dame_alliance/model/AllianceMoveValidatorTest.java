package fr.bonamy.dame_alliance.model;

import fr.bonamy.dame_alliance.util.Point;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AllianceMoveValidatorTest {

    private final AllianceMoveValidator validator = new AllianceMoveValidator();
    private final Map<Point, Piece> board = new HashMap<>();

    private void place(int a, int b, PieceColor color, PieceType type) {
        board.put(new Point(a, b), new Piece(color, type));
    }

    private Set<Point> destinations(PieceColor color) {
        return validator.getLegalMoves(color, board).stream().map(Move::to).collect(Collectors.toSet());
    }

    @Test
    void diagonalCrossPawnMovesOrthogonallyButNeverBackward() {
        place(3, 3, PieceColor.WHITE, PieceType.DIAGONAL);

        assertEquals(Set.of(new Point(3, 4), new Point(2, 3), new Point(4, 3)), destinations(PieceColor.WHITE));
    }

    @Test
    void orthogonalCrossPawnMovesDiagonallyForward() {
        place(3, 3, PieceColor.BLACK, PieceType.ORTHOGONAL);

        assertEquals(Set.of(new Point(2, 2), new Point(4, 2)), destinations(PieceColor.BLACK));
    }

    @Test
    void kingMovesInAllDirections() {
        place(3, 3, PieceColor.WHITE, PieceType.KING);

        assertEquals(8, destinations(PieceColor.WHITE).size());
    }

    @Test
    void captureBackwardIsAllowedAndMandatory() {
        place(3, 3, PieceColor.WHITE, PieceType.DIAGONAL);
        place(2, 2, PieceColor.BLACK, PieceType.ORTHOGONAL);

        Set<Move> moves = validator.getLegalMoves(PieceColor.WHITE, board);

        assertEquals(Set.of(new Move(List.of(new Point(3, 3), new Point(1, 1)), List.of(new Point(2, 2)))), moves);
    }

    @Test
    void pawnCapturesOnlyAlongItsCross() {
        place(3, 3, PieceColor.WHITE, PieceType.ORTHOGONAL);
        place(4, 4, PieceColor.BLACK, PieceType.ORTHOGONAL);

        assertTrue(validator.getLegalMoves(PieceColor.WHITE, board).stream().noneMatch(Move::isCapture));
    }

    @Test
    void longestCaptureTakesPriority() {
        place(1, 1, PieceColor.WHITE, PieceType.ORTHOGONAL);
        place(1, 2, PieceColor.BLACK, PieceType.ORTHOGONAL);
        place(1, 4, PieceColor.BLACK, PieceType.ORTHOGONAL);
        place(2, 1, PieceColor.BLACK, PieceType.ORTHOGONAL);

        Set<Move> moves = validator.getLegalMoves(PieceColor.WHITE, board);

        assertEquals(Set.of(new Move(
                List.of(new Point(1, 1), new Point(1, 3), new Point(1, 5)),
                List.of(new Point(1, 2), new Point(1, 4)))), moves);
    }

    @Test
    void jumpedPieceStaysOnBoardUntilEndOfCapture() {
        place(3, 3, PieceColor.WHITE, PieceType.KING);
        place(4, 3, PieceColor.BLACK, PieceType.DIAGONAL);
        place(5, 4, PieceColor.BLACK, PieceType.DIAGONAL);

        Set<Move> moves = validator.getLegalMoves(PieceColor.WHITE, board);

        assertTrue(moves.stream().allMatch(move -> Set.copyOf(move.captured()).size() == move.captured().size()));
        assertTrue(moves.stream().allMatch(move -> move.captured().size() == 2));
    }

    @Test
    void pawnCannotCaptureKing() {
        place(3, 3, PieceColor.WHITE, PieceType.DIAGONAL);
        place(4, 4, PieceColor.BLACK, PieceType.KING);

        assertTrue(validator.getLegalMoves(PieceColor.WHITE, board).stream().noneMatch(Move::isCapture));
    }

    @Test
    void kingCanCaptureKing() {
        place(3, 3, PieceColor.WHITE, PieceType.KING);
        place(4, 4, PieceColor.BLACK, PieceType.KING);

        assertEquals(Set.of(new Point(5, 5)), destinations(PieceColor.WHITE));
    }

    @Test
    void pawnIsPromotedOnOpponentLastRow() {
        Piece white = new Piece(PieceColor.WHITE, PieceType.DIAGONAL);
        Piece black = new Piece(PieceColor.BLACK, PieceType.DIAGONAL);

        assertTrue(validator.canBePromoted(white, new Point(2, GameConfig.getBoardSize() - 1)));
        assertFalse(validator.canBePromoted(white, new Point(2, 0)));
        assertTrue(validator.canBePromoted(black, new Point(2, 0)));
    }
}
