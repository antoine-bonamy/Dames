package fr.bonamy.dame_alliance.model;

import fr.bonamy.dame_alliance.util.Point;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private final MoveValidator validator = new AllianceMoveValidator();

    @Test
    void newGameStartsWithWhite() {
        Game game = new Game(validator);

        assertEquals(PieceColor.WHITE, game.getCurrentPlayer());
        assertEquals(GamePhase.PLAYING, game.getPhase());
        assertEquals(24, game.getPieces().size());
    }

    @Test
    void movedPawnMustBeOrientedBeforeTurnEnds() {
        Game game = new Game(validator);
        Point from = new Point(3, 2);
        Point to = new Point(3, 3);

        game.play(game.findMove(List.of(from, to)).orElseThrow());

        assertEquals(GamePhase.ORIENTING, game.getPhase());
        assertEquals(PieceColor.WHITE, game.getCurrentPlayer());

        game.orient(PieceType.ORTHOGONAL);

        assertEquals(PieceType.ORTHOGONAL, game.getPieces().get(to).getType());
        assertEquals(PieceColor.BLACK, game.getCurrentPlayer());
        assertEquals(GamePhase.PLAYING, game.getPhase());
    }

    @Test
    void illegalMoveIsRejectedAndTurnDoesNotChange() {
        Game game = new Game(validator);

        assertTrue(game.findMove(List.of(new Point(3, 2), new Point(3, 4))).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> game.play(new Move(List.of(new Point(3, 2), new Point(3, 4)), List.of())));
        assertEquals(PieceColor.WHITE, game.getCurrentPlayer());
    }

    @Test
    void nextStepsFollowMultipleCapturePath() {
        Game game = new Game(validator, Map.of(
                new Point(1, 1), new Piece(PieceColor.WHITE, PieceType.ORTHOGONAL),
                new Point(1, 2), new Piece(PieceColor.BLACK, PieceType.ORTHOGONAL),
                new Point(1, 4), new Piece(PieceColor.BLACK, PieceType.ORTHOGONAL),
                new Point(6, 6), new Piece(PieceColor.BLACK, PieceType.ORTHOGONAL)), PieceColor.WHITE);

        assertEquals(Set.of(new Point(1, 3)), game.getNextSteps(List.of(new Point(1, 1))));
        assertEquals(Set.of(new Point(1, 5)), game.getNextSteps(List.of(new Point(1, 1), new Point(1, 3))));
        assertTrue(game.findMove(List.of(new Point(1, 1), new Point(1, 3))).isEmpty());

        game.play(game.findMove(List.of(new Point(1, 1), new Point(1, 3), new Point(1, 5))).orElseThrow());

        assertEquals(Set.of(new Point(1, 5), new Point(6, 6)), game.getPieces().keySet());
    }

    @Test
    void promotedPawnSkipsOrientation() {
        Game game = new Game(validator, Map.of(
                new Point(3, 6), new Piece(PieceColor.WHITE, PieceType.DIAGONAL),
                new Point(0, 0), new Piece(PieceColor.BLACK, PieceType.KING)), PieceColor.WHITE);

        game.play(game.findMove(List.of(new Point(3, 6), new Point(3, 7))).orElseThrow());

        assertEquals(PieceType.KING, game.getPieces().get(new Point(3, 7)).getType());
        assertEquals(PieceColor.BLACK, game.getCurrentPlayer());
        assertEquals(GamePhase.PLAYING, game.getPhase());
    }

    @Test
    void playerWithoutLegalMoveLoses() {
        Game game = new Game(validator, Map.of(
                new Point(0, 7), new Piece(PieceColor.BLACK, PieceType.ORTHOGONAL),
                new Point(1, 6), new Piece(PieceColor.WHITE, PieceType.KING)), PieceColor.BLACK);

        assertEquals(GamePhase.OVER, game.getPhase());
        assertEquals(PieceColor.WHITE, game.getWinner());
    }

    @Test
    void kingCountSettlesKingsOnlyPosition() {
        Game game = new Game(validator, Map.of(
                new Point(0, 0), new Piece(PieceColor.WHITE, PieceType.KING),
                new Point(7, 7), new Piece(PieceColor.BLACK, PieceType.KING),
                new Point(7, 0), new Piece(PieceColor.BLACK, PieceType.KING)), PieceColor.WHITE);

        assertTrue(game.canSettleByKingCount());
        game.settleByKingCount();

        assertEquals(GamePhase.OVER, game.getPhase());
        assertEquals(PieceColor.BLACK, game.getWinner());
    }

    @Test
    void copyIsIndependentFromOriginal() {
        Game game = new Game(validator);
        Game copy = game.copy();

        copy.play(copy.findMove(List.of(new Point(3, 2), new Point(3, 3))).orElseThrow());
        copy.orient(PieceType.ORTHOGONAL);

        assertEquals(PieceType.DIAGONAL, game.getPieces().get(new Point(3, 2)).getType());
        assertFalse(game.getPieces().containsKey(new Point(3, 3)));
        assertEquals(PieceColor.WHITE, game.getCurrentPlayer());
        assertEquals(PieceColor.BLACK, copy.getCurrentPlayer());
    }

    @Test
    void restoreReplacesPositionAndTurn() {
        Game game = new Game(validator);

        game.restore(Map.of(
                new Point(2, 2), new Piece(PieceColor.WHITE, PieceType.KING),
                new Point(5, 5), new Piece(PieceColor.BLACK, PieceType.DIAGONAL)), PieceColor.BLACK);

        assertEquals(2, game.getPieces().size());
        assertEquals(PieceColor.BLACK, game.getCurrentPlayer());
        assertEquals(GamePhase.PLAYING, game.getPhase());
        assertNull(game.getWinner());
    }

    @Test
    void kingCountIsUnavailableWhilePawnsRemain() {
        Game game = new Game(validator);

        assertFalse(game.canSettleByKingCount());
        assertThrows(IllegalStateException.class, game::settleByKingCount);
    }
}
