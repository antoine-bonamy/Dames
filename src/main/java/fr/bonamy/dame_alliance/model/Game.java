package fr.bonamy.dame_alliance.model;

import fr.bonamy.dame_alliance.util.Point;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * État d'une partie. Un tour se déroule en deux temps : jouer un coup complet ({@link #play}),
 * puis orienter le pion joué ({@link #orient}), sauf s'il est devenu roi.
 */
public class Game {

    private final MoveValidator moveValidator;
    private Map<Point, Piece> pieces;
    private PieceColor currentPlayer;
    private GamePhase phase;
    private Set<Move> legalMoves;
    private Point pieceToOrient;
    private PieceColor winner;

    public Game(MoveValidator moveValidator) {
        this.moveValidator = moveValidator;
        reset();
    }

    /**
     * Démarre une partie depuis une position arbitraire.
     */
    public Game(MoveValidator moveValidator, Map<Point, Piece> pieces, PieceColor firstPlayer) {
        this.moveValidator = moveValidator;
        start(copyPieces(pieces), firstPlayer);
    }

    private Game(Game other) {
        this.moveValidator = other.moveValidator;
        this.pieces = copyPieces(other.pieces);
        this.currentPlayer = other.currentPlayer;
        this.phase = other.phase;
        this.legalMoves = other.legalMoves;
        this.pieceToOrient = other.pieceToOrient;
        this.winner = other.winner;
    }

    /**
     * Copie indépendante, pièces comprises (elles sont mutables), pour simuler des coups sans toucher à la partie.
     */
    public Game copy() {
        return new Game(this);
    }

    private static Map<Point, Piece> copyPieces(Map<Point, Piece> pieces) {
        Map<Point, Piece> copy = new HashMap<>();
        pieces.forEach((position, piece) -> copy.put(position, piece.copy()));
        return copy;
    }

    public void reset() {
        start(initialPieces(), PieceColor.WHITE);
    }

    /**
     * Remplace la partie en cours par une position donnée, en début de tour de {@code player}.
     */
    public void restore(Map<Point, Piece> pieces, PieceColor player) {
        start(copyPieces(pieces), player);
    }

    private void start(Map<Point, Piece> pieces, PieceColor firstPlayer) {
        this.pieces = pieces;
        this.currentPlayer = firstPlayer;
        this.pieceToOrient = null;
        this.winner = null;
        startTurn();
    }

    private static Map<Point, Piece> initialPieces() {
        Map<Point, Piece> pieces = new HashMap<>();
        for (int i = 1; i < 7; i++) {
            pieces.put(new Point(i, 1), new Piece(PieceColor.WHITE, PieceType.ORTHOGONAL));
            pieces.put(new Point(i, 2), new Piece(PieceColor.WHITE, PieceType.DIAGONAL));
            pieces.put(new Point(i, 5), new Piece(PieceColor.BLACK, PieceType.DIAGONAL));
            pieces.put(new Point(i, 6), new Piece(PieceColor.BLACK, PieceType.ORTHOGONAL));
        }
        return pieces;
    }

    /**
     * Cases atteignables à l'étape suivante d'un chemin en cours (case de départ incluse),
     * en ne gardant que les chemins qui mènent à un coup légal.
     */
    public Set<Point> getNextSteps(List<Point> partialPath) {
        if (phase != GamePhase.PLAYING) {
            return Set.of();
        }
        return legalMoves.stream()
                .map(Move::path)
                .filter(path -> path.size() > partialPath.size()
                        && path.subList(0, partialPath.size()).equals(partialPath))
                .map(path -> path.get(partialPath.size()))
                .collect(Collectors.toSet());
    }

    public Optional<Move> findMove(List<Point> path) {
        return legalMoves.stream().filter(move -> move.path().equals(path)).findFirst();
    }

    public void play(Move move) {
        if (phase != GamePhase.PLAYING || !legalMoves.contains(move)) {
            throw new IllegalArgumentException("Coup illégal : " + move);
        }
        Piece piece = pieces.remove(move.from());
        move.captured().forEach(pieces::remove);
        pieces.put(move.to(), piece);

        if (moveValidator.canBePromoted(piece, move.to())) {
            piece.makeKing();
        }
        if (piece.getType() == PieceType.KING) {
            endTurn();
        } else {
            pieceToOrient = move.to();
            phase = GamePhase.ORIENTING;
        }
    }

    public void orient(PieceType orientation) {
        if (phase != GamePhase.ORIENTING || orientation == PieceType.KING) {
            throw new IllegalStateException("Aucun pion à orienter en " + orientation);
        }
        pieces.get(pieceToOrient).setType(orientation);
        pieceToOrient = null;
        endTurn();
    }

    /**
     * Fin à l'amiable possible uniquement quand il ne reste que des rois.
     */
    public boolean canSettleByKingCount() {
        return phase == GamePhase.PLAYING
                && pieces.values().stream().allMatch(piece -> piece.getType() == PieceType.KING);
    }

    public void settleByKingCount() {
        if (!canSettleByKingCount()) {
            throw new IllegalStateException("Il reste des pions sur le plateau");
        }
        long whiteKings = countPieces(PieceColor.WHITE);
        long blackKings = countPieces(PieceColor.BLACK);
        winner = whiteKings == blackKings ? null
                : whiteKings > blackKings ? PieceColor.WHITE : PieceColor.BLACK;
        phase = GamePhase.OVER;
    }

    public boolean isCaptureRequired() {
        return legalMoves.stream().anyMatch(Move::isCapture);
    }

    private long countPieces(PieceColor color) {
        return pieces.values().stream().filter(piece -> piece.getColor() == color).count();
    }

    private void endTurn() {
        currentPlayer = currentPlayer.opponent();
        startTurn();
    }

    private void startTurn() {
        legalMoves = moveValidator.getLegalMoves(currentPlayer, pieces);
        if (legalMoves.isEmpty()) {
            winner = currentPlayer.opponent();
            phase = GamePhase.OVER;
        } else {
            phase = GamePhase.PLAYING;
        }
    }

    public Set<Move> getLegalMoves() {
        return Collections.unmodifiableSet(legalMoves);
    }

    public Map<Point, Piece> getPieces() {
        return Collections.unmodifiableMap(pieces);
    }

    public PieceColor getCurrentPlayer() {
        return currentPlayer;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public Point getPieceToOrient() {
        return pieceToOrient;
    }

    /**
     * @return le vainqueur, ou {@code null} si la partie n'est pas terminée ou est nulle
     */
    public PieceColor getWinner() {
        return winner;
    }
}
