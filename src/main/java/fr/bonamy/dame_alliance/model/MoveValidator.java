package fr.bonamy.dame_alliance.model;

import fr.bonamy.dame_alliance.util.Point;

import java.util.Map;
import java.util.Set;

public interface MoveValidator {

    /**
     * Coups jouables par {@code color}, contraintes de prise obligatoire et de rafle maximale déjà appliquées.
     */
    Set<Move> getLegalMoves(PieceColor color, Map<Point, Piece> boardState);

    boolean canBePromoted(Piece piece, Point to);
}
