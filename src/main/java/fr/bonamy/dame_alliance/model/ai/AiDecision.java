package fr.bonamy.dame_alliance.model.ai;

import fr.bonamy.dame_alliance.model.Move;
import fr.bonamy.dame_alliance.model.PieceType;

/**
 * Tour complet choisi par l'IA.
 *
 * @param orientation orientation à donner au pion joué, {@code null} s'il vient d'être promu roi
 */
public record AiDecision(Move move, PieceType orientation) {
}
