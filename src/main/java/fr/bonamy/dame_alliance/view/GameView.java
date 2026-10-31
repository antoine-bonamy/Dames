package fr.bonamy.dame_alliance.view;

import fr.bonamy.dame_alliance.model.Piece;
import fr.bonamy.dame_alliance.model.PieceColor;
import fr.bonamy.dame_alliance.model.PieceType;
import fr.bonamy.dame_alliance.model.ai.Difficulty;
import fr.bonamy.dame_alliance.util.Point;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import fr.bonamy.dame_alliance.persistence.GameFileStore;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

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
    private final Button saveButton = new Button("Sauvegarder");
    private final Button loadButton = new Button("Charger");
    private final Label noticeLabel = new Label();
    private File lastDirectory = new File(System.getProperty("user.home"));
    private final HBox thinkingBox = new HBox(8);
    private final CheckBox aiEnabledBox = new CheckBox("Jouer contre l'IA");
    private final ComboBox<PieceColor> humanColorBox = new ComboBox<>();
    private final ComboBox<Difficulty> difficultyBox = new ComboBox<>();

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
                + "dans les autres directions, jamais en arrière.");
        rules.getStyleClass().add("rules");
        rules.setWrapText(true);
        rules.setMinHeight(Region.USE_PREF_SIZE);
        // Le panneau manque de hauteur pour afficher à la fois l'aide et le choix d'orientation.
        rules.visibleProperty().bind(orientationSection.visibleProperty().not());
        rules.managedProperty().bind(rules.visibleProperty());

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(18, 18);
        Label thinkingLabel = new Label("L'IA réfléchit…");
        thinkingLabel.getStyleClass().add("thinking");
        thinkingBox.getChildren().addAll(spinner, thinkingLabel);
        thinkingBox.setAlignment(Pos.CENTER_LEFT);
        thinkingBox.getStyleClass().add("thinking-box");
        showAiThinking(false);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        kingCountButton.getStyleClass().add("secondary-button");
        newGameButton.getStyleClass().add("primary-button");

        saveButton.getStyleClass().add("secondary-button");
        loadButton.getStyleClass().add("secondary-button");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        loadButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(saveButton, Priority.ALWAYS);
        HBox.setHgrow(loadButton, Priority.ALWAYS);
        HBox fileButtons = new HBox(8, saveButton, loadButton);
        noticeLabel.getStyleClass().add("notice");
        noticeLabel.setWrapText(true);
        showNotice(null);

        VBox panel = new VBox(14, title, turnBox, hintLabel, thinkingBox, orientationSection, rules, spacer,
                buildSettingsSection(), kingCountButton, fileButtons, noticeLabel, newGameButton);
        panel.getStyleClass().add("side-panel");
        panel.setPrefWidth(250);
        BorderPane.setMargin(panel, new Insets(0, 0, 0, 24));
        panel.getChildren().filtered(node -> node instanceof Button)
                .forEach(button -> ((Button) button).setMaxWidth(Double.MAX_VALUE));
        orthogonalButton.setMaxWidth(Double.MAX_VALUE);
        diagonalButton.setMaxWidth(Double.MAX_VALUE);
        return panel;
    }

    private VBox buildSettingsSection() {
        Label sectionTitle = new Label("Prochaine partie");
        sectionTitle.getStyleClass().add("section-title");

        humanColorBox.getItems().setAll(PieceColor.WHITE, PieceColor.BLACK);
        humanColorBox.setValue(PieceColor.WHITE);
        humanColorBox.setConverter(labelConverter(color -> color == PieceColor.WHITE ? "Blancs" : "Noirs"));
        difficultyBox.getItems().setAll(Difficulty.values());
        difficultyBox.setValue(Difficulty.MEDIUM);
        difficultyBox.setConverter(labelConverter(difficulty -> switch (difficulty) {
            case EASY -> "Facile";
            case MEDIUM -> "Moyen";
            case HARD -> "Difficile";
        }));
        aiEnabledBox.setSelected(true);
        humanColorBox.disableProperty().bind(aiEnabledBox.selectedProperty().not());
        difficultyBox.disableProperty().bind(aiEnabledBox.selectedProperty().not());

        GridPane options = new GridPane();
        options.setHgap(10);
        options.setVgap(8);
        options.addRow(0, new Label("Vous jouez"), humanColorBox);
        options.addRow(1, new Label("Niveau"), difficultyBox);
        humanColorBox.setMaxWidth(Double.MAX_VALUE);
        difficultyBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(humanColorBox, Priority.ALWAYS);
        GridPane.setHgrow(difficultyBox, Priority.ALWAYS);

        VBox section = new VBox(8, sectionTitle, aiEnabledBox, options);
        section.getStyleClass().add("settings-section");
        return section;
    }

    private static <T> StringConverter<T> labelConverter(Function<T, String> label) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : label.apply(value);
            }

            @Override
            public T fromString(String text) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public GameSettings getSettings() {
        PieceColor aiColor = aiEnabledBox.isSelected() ? humanColorBox.getValue().opponent() : null;
        return new GameSettings(aiColor, difficultyBox.getValue());
    }

    public void setSettings(GameSettings settings) {
        aiEnabledBox.setSelected(settings.aiColor() != null);
        if (settings.aiColor() != null) {
            humanColorBox.setValue(settings.aiColor().opponent());
            difficultyBox.setValue(settings.difficulty());
        }
    }

    public void setSaveEnabled(boolean enabled) {
        saveButton.setDisable(!enabled);
    }

    public void showNotice(String notice) {
        noticeLabel.setText(notice);
        noticeLabel.setVisible(notice != null);
        noticeLabel.setManaged(notice != null);
    }

    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.setTitle("Dame Alliance");
        alert.initOwner(getScene().getWindow());
        alert.showAndWait();
    }

    private FileChooser fileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Partie Dame Alliance (*" + GameFileStore.FILE_EXTENSION + ")", "*" + GameFileStore.FILE_EXTENSION));
        if (lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        return chooser;
    }

    public void setOnSave(Consumer<Path> onSave) {
        saveButton.setOnAction(_ -> {
            FileChooser chooser = fileChooser("Sauvegarder la partie");
            chooser.setInitialFileName("partie" + GameFileStore.FILE_EXTENSION);
            File file = chooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
                lastDirectory = file.getParentFile();
                // Certains sélecteurs natifs (GTK) n'ajoutent pas l'extension du filtre.
                if (!file.getName().endsWith(GameFileStore.FILE_EXTENSION)) {
                    file = new File(file.getPath() + GameFileStore.FILE_EXTENSION);
                }
                onSave.accept(file.toPath());
            }
        });
    }

    public void setOnLoad(Consumer<Path> onLoad) {
        loadButton.setOnAction(_ -> {
            File file = fileChooser("Charger une partie").showOpenDialog(getScene().getWindow());
            if (file != null) {
                lastDirectory = file.getParentFile();
                onLoad.accept(file.toPath());
            }
        });
    }

    public void showAiThinking(boolean thinking) {
        thinkingBox.setVisible(thinking);
        thinkingBox.setManaged(thinking);
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

    public void highlight(Collection<Point> lastMove, Collection<Point> selectedPath, Collection<Point> nextSteps) {
        boardView.clearBoard();
        boardView.markLastMove(lastMove);
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
