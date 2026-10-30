package fr.bonamy.dame_alliance.controller;

import fr.bonamy.dame_alliance.model.*;
import fr.bonamy.dame_alliance.util.Point;
import fr.bonamy.dame_alliance.view.GameView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Traduit les clics en coups. Une rafle se saisit case par case : le chemin partiel n'est transmis
 * au modèle qu'une fois qu'il correspond à un coup légal complet.
 */
public class GameController {

    private final GameView gameView;
    private final Game game;

    private List<Point> currentPath = List.of();

    public GameController(Game game, GameView gameView) {
        this.game = game;
        this.gameView = gameView;
        gameView.setOnPieceClicked(this::handlePieceClick);
        gameView.setOnTileClicked(this::handleTileClick);
        gameView.setOnOrientationChosen(this::handleOrientation);
        gameView.setOnKingCount(this::handleKingCount);
        gameView.setOnNewGame(this::handleNewGame);
        refresh();
    }

    private void handlePieceClick(Point point) {
        if (game.getPhase() != GamePhase.PLAYING) {
            return;
        }
        // Une rafle circulaire peut revenir sur sa case de départ, encore occupée par le pion joué.
        if (currentPath.size() > 1 && game.getNextSteps(currentPath).contains(point)) {
            handleTileClick(point);
            return;
        }
        currentPath = List.of(point);
        refreshHighlights();
    }

    private void handleTileClick(Point tilePosition) {
        if (game.getPhase() != GamePhase.PLAYING || currentPath.isEmpty()) {
            return;
        }
        if (!game.getNextSteps(currentPath).contains(tilePosition)) {
            // Un clic hors cible annule une simple sélection, mais pas une rafle déjà entamée.
            if (currentPath.size() == 1) {
                currentPath = List.of();
            }
            refreshHighlights();
            return;
        }
        List<Point> path = new ArrayList<>(currentPath);
        path.add(tilePosition);
        currentPath = List.copyOf(path);

        Optional<Move> move = game.findMove(currentPath);
        if (move.isPresent()) {
            game.play(move.get());
            currentPath = List.of();
            refresh();
        } else {
            refreshHighlights();
        }
    }

    private void handleOrientation(PieceType orientation) {
        if (game.getPhase() == GamePhase.ORIENTING) {
            game.orient(orientation);
            refresh();
        }
    }

    private void handleKingCount() {
        if (game.canSettleByKingCount()) {
            game.settleByKingCount();
            currentPath = List.of();
            refresh();
        }
    }

    private void handleNewGame() {
        game.reset();
        currentPath = List.of();
        refresh();
    }

    private void refresh() {
        GamePhase phase = game.getPhase();
        gameView.renderPieces(game.getPieces(), phase == GamePhase.PLAYING ? game.getCurrentPlayer() : null);
        gameView.showOrientationChoice(phase == GamePhase.ORIENTING);
        gameView.setKingCountEnabled(game.canSettleByKingCount());
        if (phase == GamePhase.OVER) {
            gameView.showGameOver(game.getWinner());
        } else {
            gameView.showTurn(game.getCurrentPlayer(), game.isCaptureRequired());
        }
        refreshHighlights();
    }

    private void refreshHighlights() {
        switch (game.getPhase()) {
            case PLAYING -> gameView.highlight(currentPath,
                    currentPath.isEmpty() ? Set.of() : game.getNextSteps(currentPath));
            case ORIENTING -> gameView.highlight(List.of(game.getPieceToOrient()), Set.of());
            case OVER -> gameView.highlight(List.of(), Set.of());
        }
    }

}
