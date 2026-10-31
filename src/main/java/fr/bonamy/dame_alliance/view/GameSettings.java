package fr.bonamy.dame_alliance.view;

import fr.bonamy.dame_alliance.model.PieceColor;
import fr.bonamy.dame_alliance.model.ai.Difficulty;

/**
 * @param aiColor couleur jouée par l'IA, {@code null} pour une partie à deux joueurs
 */
public record GameSettings(PieceColor aiColor, Difficulty difficulty) {
}
