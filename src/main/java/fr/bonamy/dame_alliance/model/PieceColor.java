package fr.bonamy.dame_alliance.model;

public enum PieceColor {
    WHITE(1),
    BLACK(-1);

    private final int forward;

    PieceColor(int forward) {
        this.forward = forward;
    }

    /**
     * Sens de progression sur l'axe des lignes : les blancs partent du haut et descendent.
     */
    public int forward() {
        return forward;
    }

    public PieceColor opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
