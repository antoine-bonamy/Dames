package fr.bonamy.dame_alliance.view;

import fr.bonamy.dame_alliance.model.Piece;
import fr.bonamy.dame_alliance.model.PieceColor;
import fr.bonamy.dame_alliance.model.PieceType;
import fr.bonamy.dame_alliance.util.Point;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public class GameView extends BorderPane {

    private static final double PREVIEW_RADIUS = 14;

    private final BoardView boardView;

    private final Circle turnIndicator = new Circle(9);
    private final Label turnLabel = new Label();
    private final Label hintLabel = new Label();
    private final VBox orientationSection = new VBox(8);
    private final Button orthogonalButton = new Button("Croix droite");
    private final Button diagonalButton = new Button("Croix en X");
    private final Button kingCountButton = new Button("Décompte des rois");
    private final Button newGameButton = new Button("Nouvelle partie");

    private Consumer<Point> onPieceClicked = _ -> {
    };
    private Consumer<PieceType> onOrientationChosen = _ -> {
    };

    public GameView(BoardView boardView) {
        super();
        this.boardView = boardView;
        getStyleClass().add("game");
        setCenter(boardView);
        setRight(buildSidePanel());

        orthogonalButton.setOnAction(_ -> onOrientationChosen.accept(PieceType.ORTHOGONAL));
        diagonalButton.setOnAction(_ -> onOrientationChosen.accept(PieceType.DIAGONAL));
        showOrientationChoice(false);
    }

    private VBox buildSidePanel() {
        Label title = new Label("Dame Alliance");
        title.getStyleClass().add("title");

        turnIndicator.getStyleClass().add("turn-indicator");
        turnLabel.getStyleClass().add("turn-label");
        HBox turnBox = new HBox(10, turnIndicator, turnLabel);
        turnBox.setAlignment(Pos.CENTER_LEFT);
        hintLabel.getStyleClass().add("hint");
        hintLabel.setWrapText(true);

        Label orientationTitle = new Label("Orienter le pion joué");
        orientationTitle.getStyleClass().add("section-title");
        orthogonalButton.getStyleClass().add("orientation-button");
        diagonalButton.getStyleClass().add("orientation-button");
        orientationSection.getChildren().addAll(orientationTitle, orthogonalButton, diagonalButton);
        orientationSection.getStyleClass().add("orientation-section");

        Label rules = new Label("La croix indique les directions de prise. Les pions se déplacent "
                + "dans les autres directions, jamais en arrière. La prise est obligatoire, rafle la plus longue.");
        rules.getStyleClass().add("rules");
        rules.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        kingCountButton.getStyleClass().add("secondary-button");
        newGameButton.getStyleClass().add("primary-button");

        VBox panel = new VBox(14, title, turnBox, hintLabel, orientationSection, rules, spacer,
                kingCountButton, newGameButton);
        panel.getStyleClass().add("side-panel");
        panel.setPrefWidth(250);
        BorderPane.setMargin(panel, new Insets(0, 0, 0, 24));
        panel.getChildren().filtered(node -> node instanceof Button)
                .forEach(button -> ((Button) button).setMaxWidth(Double.MAX_VALUE));
        orthogonalButton.setMaxWidth(Double.MAX_VALUE);
        diagonalButton.setMaxWidth(Double.MAX_VALUE);
        return panel;
    }

    /**
     * @param activeColor couleur dont les pions sont cliquables, ou {@code null} pour n'en rendre aucun cliquable
     */
    public void renderPieces(Map<Point, Piece> pieces, PieceColor activeColor) {
        boardView.clearPieces();
        for (Map.Entry<Point, Piece> pieceEntry : pieces.entrySet()) {
            PieceView pieceView = new PieceView(pieceEntry.getValue().getColor(), pieceEntry.getValue().getType());
            Point point = pieceEntry.getKey();
            if (pieceEntry.getValue().getColor() == activeColor) {
                pieceView.setPlayable(true);
                pieceView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                    onPieceClicked.accept(point);
                    event.consume();
                });
            } else {
                pieceView.setMouseTransparent(true);
            }
            boardView.placePiece(point, pieceView);
        }
    }

    public void highlight(Collection<Point> selectedPath, Collection<Point> nextSteps) {
        boardView.clearBoard();
        boardView.markSelected(selectedPath);
        boardView.markTargets(nextSteps);
    }

    public void showTurn(PieceColor player, boolean captureRequired) {
        turnLabel.setText("Au tour des " + colorName(player));
        setIndicatorColor(player);
        showHint(captureRequired ? "Prise obligatoire : jouez la rafle la plus longue." : null);
        orthogonalButton.setGraphic(new PieceView(player, PieceType.ORTHOGONAL, PREVIEW_RADIUS));
        diagonalButton.setGraphic(new PieceView(player, PieceType.DIAGONAL, PREVIEW_RADIUS));
    }

    public void showGameOver(PieceColor winner) {
        turnLabel.setText(winner == null ? "Partie nulle" : "Victoire des " + colorName(winner) + " !");
        turnIndicator.setVisible(winner != null);
        if (winner != null) {
            setIndicatorColor(winner);
        }
        showHint("Lancez une nouvelle partie pour rejouer.");
    }

    private void showHint(String hint) {
        hintLabel.setText(hint);
        hintLabel.setVisible(hint != null);
        hintLabel.setManaged(hint != null);
    }

    private void setIndicatorColor(PieceColor color) {
        turnIndicator.setVisible(true);
        turnIndicator.getStyleClass().removeAll("white", "black");
        turnIndicator.getStyleClass().add(color == PieceColor.WHITE ? "white" : "black");
    }

    public void showOrientationChoice(boolean visible) {
        orientationSection.setVisible(visible);
        orientationSection.setManaged(visible);
    }

    public void setKingCountEnabled(boolean enabled) {
        kingCountButton.setDisable(!enabled);
    }

    private static String colorName(PieceColor color) {
        return color == PieceColor.WHITE ? "blancs" : "noirs";
    }

    public void setOnTileClicked(Consumer<Point> onTileClicked) {
        boardView.setOnTileClicked(onTileClicked);
    }

    public void setOnPieceClicked(Consumer<Point> onPieceClicked) {
        this.onPieceClicked = onPieceClicked;
    }

    public void setOnOrientationChosen(Consumer<PieceType> onOrientationChosen) {
        this.onOrientationChosen = onOrientationChosen;
    }

    public void setOnKingCount(Runnable onKingCount) {
        kingCountButton.setOnAction(_ -> onKingCount.run());
    }

    public void setOnNewGame(Runnable onNewGame) {
        newGameButton.setOnAction(_ -> onNewGame.run());
    }

}
