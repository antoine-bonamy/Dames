package fr.bonamy.dame_alliance.model.ai;

import fr.bonamy.dame_alliance.model.AllianceMoveValidator;
import fr.bonamy.dame_alliance.model.Game;
import fr.bonamy.dame_alliance.model.GamePhase;
import fr.bonamy.dame_alliance.model.Piece;
import fr.bonamy.dame_alliance.model.PieceColor;
import fr.bonamy.dame_alliance.model.PieceType;
import fr.bonamy.dame_alliance.util.Point;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AiPlayerTest {

    private final AllianceMoveValidator validator = new AllianceMoveValidator();

    @Test
    void choosesALegalTurnFromInitialPosition() {
        Game game = new Game(validator);

        for (Difficulty difficulty : Difficulty.values()) {
            AiDecision decision = new AiPlayer(difficulty, new Random(42)).chooseTurn(game);

            assertTrue(game.getLegalMoves().contains(decision.move()));
            assertNotNull(decision.orientation());
        }
    }

    @Test
    void doesNotAlterTheGameItAnalyses() {
        Game game = new Game(validator);
        Map<Point, Piece> before = Map.copyOf(game.getPieces());

        new AiPlayer(Difficulty.MEDIUM, new Random(1)).chooseTurn(game);

        assertEquals(before, game.getPieces());
        assertEquals(PieceColor.WHITE, game.getCurrentPlayer());
        assertEquals(GamePhase.PLAYING, game.getPhase());
    }

    @Test
    void prefersPromotionOverSidewaysMove() {
        Game game = new Game(validator, Map.of(
                new Point(3, 6), new Piece(PieceColor.WHITE, PieceType.DIAGONAL),
                new Point(0, 0), new Piece(PieceColor.BLACK, PieceType.KING)), PieceColor.WHITE);

        AiDecision decision = new AiPlayer(Difficulty.MEDIUM, new Random(7)).chooseTurn(game);

        assertEquals(new Point(3, 7), decision.move().to());
        assertNull(decision.orientation());
    }

    @Test
    void avoidsLosingAPieceWhenItCan() {
        // Un pion noir orienté en croix droite capturerait le pion blanc s'il avance en (3,4).
        Game game = new Game(validator, Map.of(
                new Point(3, 3), new Piece(PieceColor.WHITE, PieceType.DIAGONAL),
                new Point(3, 5), new Piece(PieceColor.BLACK, PieceType.ORTHOGONAL),
                new Point(0, 7), new Piece(PieceColor.BLACK, PieceType.KING)), PieceColor.WHITE);

        AiDecision decision = new AiPlayer(Difficulty.MEDIUM, new Random(3)).chooseTurn(game);

        assertNotEquals(List.of(new Point(3, 3), new Point(3, 4)), decision.move().path());
    }

    @Test
    void hardLevelAnswersInReasonableTime() {
        Game game = new Game(validator);

        long start = System.nanoTime();
        new AiPlayer(Difficulty.HARD, new Random(5)).chooseTurn(game);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        System.out.println("HARD depth search took " + elapsedMillis + " ms");
        assertTrue(elapsedMillis < 10_000, "Recherche trop lente : " + elapsedMillis + " ms");
    }
}
