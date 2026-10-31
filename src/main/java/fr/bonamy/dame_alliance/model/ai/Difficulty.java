package fr.bonamy.dame_alliance.model.ai;

/**
 * Profondeur de recherche en tours complets, et amplitude du bruit ajouté aux scores pour rendre l'IA faillible.
 */
public enum Difficulty {
    EASY(1, 150),
    MEDIUM(3, 0),
    HARD(5, 0);

    private final int depth;
    private final int noise;

    Difficulty(int depth, int noise) {
        this.depth = depth;
        this.noise = noise;
    }

    public int getDepth() {
        return depth;
    }

    public int getNoise() {
        return noise;
    }
}
