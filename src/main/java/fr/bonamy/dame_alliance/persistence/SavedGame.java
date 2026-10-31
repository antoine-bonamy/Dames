package fr.bonamy.dame_alliance.persistence;

import fr.bonamy.dame_alliance.model.Piece;
import fr.bonamy.dame_alliance.model.PieceColor;
import fr.bonamy.dame_alliance.model.ai.Difficulty;
import fr.bonamy.dame_alliance.util.Point;

import java.util.Map;
import java.util.Objects;

/**
 * Partie sauvegardée en début de tour.
 *
 * @param aiColor    couleur jouée par l'IA, {@code null} pour une partie à deux joueurs
 * @param difficulty niveau de l'IA, {@code null} si et seulement si {@code aiColor} l'est
 */
public record SavedGame(Map<Point, Piece> pieces, PieceColor turn, PieceColor aiColor, Difficulty difficulty) {

    public SavedGame {
        Objects.requireNonNull(pieces);
        Objects.requireNonNull(turn);
        if ((aiColor == null) != (difficulty == null)) {
            throw new IllegalArgumentException("La couleur et le niveau de l'IA vont de pair");
        }
        pieces = Map.copyOf(pieces);
    }
}
