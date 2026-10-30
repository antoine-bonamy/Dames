package fr.bonamy.dame_alliance.view;

import fr.bonamy.dame_alliance.model.PieceColor;
import fr.bonamy.dame_alliance.model.PieceType;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

/**
 * Pion dessiné en vectoriel. Les formes sont dimensionnées pour un rayon de référence
 * puis mises à l'échelle, afin de réutiliser le même dessin en miniature.
 */
public class PieceView extends StackPane {

    private static final double REFERENCE_RADIUS = 28;
    private static final String CROWN_PATH =
            "M -15 7 L -17 -9 L -8 -2 L 0 -13 L 8 -2 L 17 -9 L 15 7 Z M -15 10 L 15 10 L 15 14 L -15 14 Z";

    private final PieceColor pieceColor;
    private final PieceType pieceType;

    public PieceView(PieceColor pieceColor, PieceType pieceType) {
        this(pieceColor, pieceType, REFERENCE_RADIUS);
    }

    public PieceView(PieceColor pieceColor, PieceType pieceType, double radius) {
        this.pieceColor = pieceColor;
        this.pieceType = pieceType;
        getStyleClass().addAll("piece", pieceColor == PieceColor.WHITE ? "piece-white" : "piece-black");

        Circle body = new Circle(REFERENCE_RADIUS);
        body.getStyleClass().add("piece-body");
        Circle rim = new Circle(REFERENCE_RADIUS * 0.72);
        rim.getStyleClass().add("piece-rim");

        Group drawing = new Group(body, rim, pieceType == PieceType.KING ? buildCrown() : buildCross());
        double scale = radius / REFERENCE_RADIUS;
        drawing.setScaleX(scale);
        drawing.setScaleY(scale);
        getChildren().add(new Group(drawing));
    }

    private Group buildCross() {
        Rectangle horizontal = crossBar();
        Rectangle vertical = crossBar();
        vertical.setRotate(90);
        Group cross = new Group(horizontal, vertical);
        if (pieceType == PieceType.DIAGONAL) {
            cross.setRotate(45);
        }
        return cross;
    }

    private static Rectangle crossBar() {
        Rectangle bar = new Rectangle(-14, -3, 28, 6);
        bar.setArcWidth(4);
        bar.setArcHeight(4);
        bar.getStyleClass().add("piece-mark");
        return bar;
    }

    private static SVGPath buildCrown() {
        SVGPath crown = new SVGPath();
        crown.setContent(CROWN_PATH);
        crown.getStyleClass().addAll("piece-mark", "piece-crown");
        return crown;
    }

    public void setPlayable(boolean playable) {
        getStyleClass().remove("playable");
        if (playable) {
            getStyleClass().add("playable");
        }
    }

    public PieceColor getPieceColor() {
        return pieceColor;
    }

    public PieceType getPieceType() {
        return pieceType;
    }
}
