package fr.bonamy.dame_alliance.view;

import fr.bonamy.dame_alliance.model.GameConfig;
import fr.bonamy.dame_alliance.util.Point;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Damier encadré de ses coordonnées : les cases occupent les colonnes et lignes 1 à {@code BOARD_SIZE} de la grille.
 */
public class BoardView extends GridPane {

    private final static int BOARD_SIZE = GameConfig.getBoardSize();
    private final static int TILE_SIZE_PIXEL = 72;

    private final TileView[][] tiles = new TileView[BOARD_SIZE][BOARD_SIZE];
    private Consumer<Point> onTileClicked = _ -> {
    };

    public BoardView() {
        super();
        getStyleClass().add("board");
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Point position = new Point(col, row);
                TileView tile = new TileView((col + row) % 2 == 0);
                tile.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                    onTileClicked.accept(position);
                    event.consume();
                });
                tiles[col][row] = tile;
                add(tile, col + 1, row);
            }
            add(coordinate(String.valueOf(BOARD_SIZE - row), HPos.CENTER), 0, row);
        }
        for (int col = 0; col < BOARD_SIZE; col++) {
            add(coordinate(String.valueOf((char) ('a' + col)), HPos.CENTER), col + 1, BOARD_SIZE);
        }
    }

    private static Label coordinate(String text, HPos alignment) {
        Label label = new Label(text);
        label.getStyleClass().add("coordinate");
        GridPane.setHalignment(label, alignment);
        GridPane.setValignment(label, VPos.CENTER);
        return label;
    }

    public void clearBoard() {
        for (TileView[] column : tiles) {
            for (TileView tile : column) {
                tile.setSelected(false);
                tile.setTarget(false);
            }
        }
    }

    public void markSelected(Collection<Point> positions) {
        positions.forEach(position -> tileAt(position).setSelected(true));
    }

    public void markTargets(Collection<Point> positions) {
        positions.forEach(position -> tileAt(position).setTarget(true));
    }

    public void clearPieces() {
        for (TileView[] column : tiles) {
            for (TileView tile : column) {
                tile.setPiece(null);
            }
        }
    }

    public void placePiece(Point position, PieceView pieceView) {
        tileAt(position).setPiece(pieceView);
    }

    private TileView tileAt(Point position) {
        return tiles[position.getA()][position.getB()];
    }

    public void setOnTileClicked(Consumer<Point> onTileClicked) {
        this.onTileClicked = onTileClicked;
    }

    /**
     * Case superposant, de bas en haut : le fond, le pion éventuel et le marqueur de destination.
     */
    private static class TileView extends StackPane {

        private final Circle targetMarker = new Circle(TILE_SIZE_PIXEL * 0.14);
        private PieceView piece;

        TileView(boolean light) {
            getStyleClass().addAll("tile", light ? "light" : "dark");
            setPrefSize(TILE_SIZE_PIXEL, TILE_SIZE_PIXEL);
            setMinSize(TILE_SIZE_PIXEL, TILE_SIZE_PIXEL);
            targetMarker.getStyleClass().add("target-marker");
            targetMarker.setMouseTransparent(true);
            targetMarker.setVisible(false);
            getChildren().add(targetMarker);
        }

        void setPiece(PieceView newPiece) {
            if (piece != null) {
                getChildren().remove(piece);
            }
            piece = newPiece;
            if (piece != null) {
                getChildren().addFirst(piece);
            }
        }

        void setSelected(boolean selected) {
            toggleStyleClass("selected", selected);
        }

        void setTarget(boolean target) {
            targetMarker.setVisible(target);
            toggleStyleClass("target", target);
        }

        private void toggleStyleClass(String styleClass, boolean enabled) {
            getStyleClass().remove(styleClass);
            if (enabled) {
                getStyleClass().add(styleClass);
            }
        }
    }

}
