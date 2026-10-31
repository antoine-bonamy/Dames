package fr.bonamy.dame_alliance.model.ai;

import fr.bonamy.dame_alliance.model.Game;
import fr.bonamy.dame_alliance.model.GameConfig;
import fr.bonamy.dame_alliance.model.GamePhase;
import fr.bonamy.dame_alliance.model.Move;
import fr.bonamy.dame_alliance.model.Piece;
import fr.bonamy.dame_alliance.model.PieceColor;
import fr.bonamy.dame_alliance.model.PieceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * IA par minimax (forme negamax) avec élagage alpha-bêta. Un nœud de l'arbre est un tour complet :
 * le coup puis l'orientation du pion joué. Les parties sont simulées sur des copies de {@link Game}
 * pour que les règles restent définies à un seul endroit.
 */
public class AiPlayer {

    private static final int WIN_SCORE = 1_000_000;
    private static final int PAWN_VALUE = 100;
    private static final int KING_VALUE = 300;
    private static final int ADVANCE_BONUS = 6;
    private static final PieceType[] ORIENTATIONS = {PieceType.ORTHOGONAL, PieceType.DIAGONAL};

    private final Difficulty difficulty;
    private final Random random;

    public AiPlayer(Difficulty difficulty) {
        this(difficulty, new Random());
    }

    public AiPlayer(Difficulty difficulty, Random random) {
        this.difficulty = difficulty;
        this.random = random;
    }

    public AiDecision chooseTurn(Game game) {
        if (game.getPhase() != GamePhase.PLAYING) {
            throw new IllegalStateException("L'IA ne peut jouer qu'en début de tour");
        }
        List<AiDecision> bestDecisions = new ArrayList<>();
        int bestScore = Integer.MIN_VALUE;
        int bestImmediateScore = Integer.MIN_VALUE;
        for (Child child : expand(game)) {
            int score = -negamax(child.game(), difficulty.getDepth() - 1, -WIN_SCORE * 2, WIN_SCORE * 2);
            if (difficulty.getNoise() > 0) {
                score += random.nextInt(difficulty.getNoise());
            }
            // À score égal, on préfère le gain immédiat : sinon l'IA peut repousser indéfiniment un gain qu'elle voit.
            int immediateScore = -evaluate(child.game());
            if (score > bestScore || (score == bestScore && immediateScore > bestImmediateScore)) {
                bestScore = score;
                bestImmediateScore = immediateScore;
                bestDecisions.clear();
            }
            if (score == bestScore && immediateScore == bestImmediateScore) {
                bestDecisions.add(child.decision());
            }
        }
        return bestDecisions.get(random.nextInt(bestDecisions.size()));
    }

    /**
     * @return le score du point de vue du joueur dont c'est le tour dans {@code game}
     */
    private int negamax(Game game, int depth, int alpha, int beta) {
        if (game.getPhase() == GamePhase.OVER) {
            if (game.getWinner() == null) {
                return 0;
            }
            // Le bonus de profondeur restante fait préférer les victoires rapides et les défaites lointaines.
            int score = WIN_SCORE + depth;
            return game.getWinner() == game.getCurrentPlayer() ? score : -score;
        }
        if (depth == 0) {
            return evaluate(game);
        }
        int best = -WIN_SCORE * 2;
        for (Child child : expand(game)) {
            int score = -negamax(child.game(), depth - 1, -beta, -alpha);
            best = Math.max(best, score);
            alpha = Math.max(alpha, score);
            if (alpha >= beta) {
                break;
            }
        }
        return best;
    }

    private List<Child> expand(Game game) {
        List<Child> children = new ArrayList<>();
        for (Move move : game.getLegalMoves()) {
            Game afterMove = game.copy();
            afterMove.play(move);
            if (afterMove.getPhase() != GamePhase.ORIENTING) {
                children.add(new Child(new AiDecision(move, null), afterMove));
                continue;
            }
            for (PieceType orientation : ORIENTATIONS) {
                Game afterOrientation = afterMove.copy();
                afterOrientation.orient(orientation);
                children.add(new Child(new AiDecision(move, orientation), afterOrientation));
            }
        }
        return children;
    }

    private int evaluate(Game game) {
        PieceColor player = game.getCurrentPlayer();
        int score = 0;
        for (var entry : game.getPieces().entrySet()) {
            Piece piece = entry.getValue();
            int value = piece.getType() == PieceType.KING
                    ? KING_VALUE
                    : PAWN_VALUE + ADVANCE_BONUS * rowsAdvanced(piece.getColor(), entry.getKey().getB());
            score += piece.getColor() == player ? value : -value;
        }
        return score;
    }

    private static int rowsAdvanced(PieceColor color, int row) {
        return color == PieceColor.WHITE ? row : GameConfig.getBoardSize() - 1 - row;
    }

    private record Child(AiDecision decision, Game game) {
    }
}
