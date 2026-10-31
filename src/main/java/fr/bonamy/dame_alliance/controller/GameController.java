package fr.bonamy.dame_alliance.controller;

import fr.bonamy.dame_alliance.model.*;
import fr.bonamy.dame_alliance.model.ai.AiDecision;
import fr.bonamy.dame_alliance.model.ai.AiPlayer;
import fr.bonamy.dame_alliance.persistence.GameFileStore;
import fr.bonamy.dame_alliance.persistence.SavedGame;
import fr.bonamy.dame_alliance.util.Point;
import fr.bonamy.dame_alliance.view.GameSettings;
import fr.bonamy.dame_alliance.view.GameView;
import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Traduit les clics en coups. Une rafle se saisit case par case : le chemin partiel n'est transmis
 * au modèle qu'une fois qu'il correspond à un coup légal complet.
 * Quand c'est au tour de l'IA, sa recherche tourne hors du thread JavaFX pour ne pas figer la fenêtre.
 */
public class GameController {

    private final GameView gameView;
    private final Game game;

    private List<Point> currentPath = List.of();
    private List<Point> lastMovePath = List.of();
    private final GameFileStore fileStore = new GameFileStore();
    private GameSettings settings;
    private PieceColor aiColor;
    private AiPlayer aiPlayer;
    private Task<AiDecision> aiTask;

    public GameController(Game game, GameView gameView) {
        this.game = game;
        this.gameView = gameView;
        gameView.setOnPieceClicked(this::handlePieceClick);
        gameView.setOnTileClicked(this::handleTileClick);
        gameView.setOnOrientationChosen(this::handleOrientation);
        gameView.setOnKingCount(this::handleKingCount);
        gameView.setOnNewGame(this::handleNewGame);
        gameView.setOnSave(this::handleSave);
        gameView.setOnLoad(this::handleLoad);
        applySettings(gameView.getSettings());
        refresh();
    }

    private void handlePieceClick(Point point) {
        if (!isHumanTurn()) {
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
        if (!isHumanTurn() || currentPath.isEmpty()) {
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
            lastMovePath = currentPath;
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
        if (isHumanTurn() && game.canSettleByKingCount()) {
            game.settleByKingCount();
            currentPath = List.of();
            refresh();
        }
    }

    private void handleNewGame() {
        cancelAiTurn();
        applySettings(gameView.getSettings());
        game.reset();
        currentPath = List.of();
        lastMovePath = List.of();
        refresh();
    }

    /**
     * Sauvegarde possible uniquement en début de tour humain : aucun état intermédiaire (rafle, orientation)
     * n'a ainsi besoin d'être enregistré.
     */
    private boolean canSave() {
        return isHumanTurn();
    }

    private void handleSave(Path path) {
        if (!canSave()) {
            return;
        }
        SavedGame savedGame = new SavedGame(game.getPieces(), game.getCurrentPlayer(),
                settings.aiColor(), settings.aiColor() == null ? null : settings.difficulty());
        try {
            fileStore.save(savedGame, path);
            gameView.showNotice("Partie sauvegardée : " + path.getFileName());
        } catch (IOException e) {
            gameView.showError("Impossible de sauvegarder la partie.\n" + e.getMessage());
        }
    }

    private void handleLoad(Path path) {
        SavedGame savedGame;
        try {
            savedGame = fileStore.load(path);
        } catch (IOException e) {
            gameView.showError("Impossible de charger la partie.\n" + e.getMessage());
            return;
        }
        cancelAiTurn();
        GameSettings loadedSettings = new GameSettings(savedGame.aiColor(),
                savedGame.difficulty() != null ? savedGame.difficulty() : settings.difficulty());
        gameView.setSettings(loadedSettings);
        applySettings(loadedSettings);
        game.restore(savedGame.pieces(), savedGame.turn());
        currentPath = List.of();
        lastMovePath = List.of();
        refresh();
        gameView.showNotice("Partie chargée : " + path.getFileName());
    }

    private void cancelAiTurn() {
        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }
    }

    private void applySettings(GameSettings settings) {
        this.settings = settings;
        aiColor = settings.aiColor();
        aiPlayer = aiColor == null ? null : new AiPlayer(settings.difficulty());
    }

    private boolean isAiTurn() {
        return aiPlayer != null && game.getPhase() == GamePhase.PLAYING && game.getCurrentPlayer() == aiColor;
    }

    private boolean isHumanTurn() {
        return game.getPhase() == GamePhase.PLAYING && !isAiTurn();
    }

    private void startAiTurn() {
        Game snapshot = game.copy();
        AiPlayer player = aiPlayer;
        Task<AiDecision> task = new Task<>() {
            @Override
            protected AiDecision call() {
                return player.chooseTurn(snapshot);
            }
        };
        task.setOnSucceeded(_ -> {
            if (aiTask == task) {
                aiTask = null;
                applyAiDecision(task.getValue());
            }
        });
        task.setOnFailed(_ -> {
            aiTask = null;
            task.getException().printStackTrace();
            gameView.showAiThinking(false);
        });
        aiTask = task;
        Thread thread = new Thread(task, "dame-alliance-ai");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyAiDecision(AiDecision decision) {
        game.play(decision.move());
        if (game.getPhase() == GamePhase.ORIENTING) {
            game.orient(decision.orientation());
        }
        lastMovePath = decision.move().path();
        refresh();
    }

    private void refresh() {
        GamePhase phase = game.getPhase();
        gameView.renderPieces(game.getPieces(), isHumanTurn() ? game.getCurrentPlayer() : null);
        gameView.showOrientationChoice(phase == GamePhase.ORIENTING);
        gameView.setKingCountEnabled(isHumanTurn() && game.canSettleByKingCount());
        gameView.setSaveEnabled(canSave());
        gameView.showNotice(null);
        if (phase == GamePhase.OVER) {
            gameView.showGameOver(game.getWinner());
        } else {
            gameView.showTurn(game.getCurrentPlayer(), game.isCaptureRequired());
        }
        gameView.showAiThinking(isAiTurn());
        refreshHighlights();
        if (isAiTurn() && aiTask == null) {
            startAiTurn();
        }
    }

    private void refreshHighlights() {
        switch (game.getPhase()) {
            case PLAYING -> gameView.highlight(lastMovePath, currentPath,
                    currentPath.isEmpty() ? Set.of() : game.getNextSteps(currentPath));
            case ORIENTING -> gameView.highlight(lastMovePath, List.of(game.getPieceToOrient()), Set.of());
            case OVER -> gameView.highlight(lastMovePath, List.of(), Set.of());
        }
    }

}
