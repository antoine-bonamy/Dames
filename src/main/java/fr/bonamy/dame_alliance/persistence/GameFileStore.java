package fr.bonamy.dame_alliance.persistence;

import fr.bonamy.dame_alliance.model.GameConfig;
import fr.bonamy.dame_alliance.model.Piece;
import fr.bonamy.dame_alliance.model.PieceColor;
import fr.bonamy.dame_alliance.model.PieceType;
import fr.bonamy.dame_alliance.model.ai.Difficulty;
import fr.bonamy.dame_alliance.util.Point;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lit et écrit les parties au format texte {@code .dalli} : un en-tête versionné puis une ligne par information.
 * <pre>
 * dame-alliance 1
 * turn WHITE
 * ai BLACK MEDIUM        (ou : ai NONE)
 * piece 1 2 WHITE DIAGONAL
 * </pre>
 */
public class GameFileStore {

    public static final String FILE_EXTENSION = ".dalli";

    private static final String HEADER = "dame-alliance 1";
    private static final String NO_AI = "NONE";

    public void save(SavedGame savedGame, Path path) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.add("turn " + savedGame.turn());
        lines.add(savedGame.aiColor() == null
                ? "ai " + NO_AI
                : "ai " + savedGame.aiColor() + " " + savedGame.difficulty());
        savedGame.pieces().entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<Point, Piece> entry) -> entry.getKey().getB())
                        .thenComparing(entry -> entry.getKey().getA()))
                .forEach(entry -> lines.add("piece %d %d %s %s".formatted(entry.getKey().getA(),
                        entry.getKey().getB(), entry.getValue().getColor(), entry.getValue().getType())));
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    public SavedGame load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Map<Point, Piece> pieces = new HashMap<>();
        PieceColor turn = null;
        PieceColor aiColor = null;
        Difficulty difficulty = null;
        boolean headerRead = false;
        boolean aiRead = false;

        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i).strip();
            if (line.isEmpty()) {
                continue;
            }
            if (!headerRead) {
                if (!line.equals(HEADER)) {
                    throw new InvalidSaveFileException(lineNumber, "ce n'est pas une sauvegarde Dame Alliance");
                }
                headerRead = true;
                continue;
            }
            String[] tokens = line.split("\\s+");
            switch (tokens[0]) {
                case "turn" -> {
                    expectTokens(tokens, 2, lineNumber);
                    if (turn != null) {
                        throw new InvalidSaveFileException(lineNumber, "joueur actif défini deux fois");
                    }
                    turn = parseEnum(PieceColor.class, tokens[1], lineNumber);
                }
                case "ai" -> {
                    if (aiRead) {
                        throw new InvalidSaveFileException(lineNumber, "IA définie deux fois");
                    }
                    aiRead = true;
                    if (tokens.length == 2 && tokens[1].equals(NO_AI)) {
                        continue;
                    }
                    expectTokens(tokens, 3, lineNumber);
                    aiColor = parseEnum(PieceColor.class, tokens[1], lineNumber);
                    difficulty = parseEnum(Difficulty.class, tokens[2], lineNumber);
                }
                case "piece" -> {
                    expectTokens(tokens, 5, lineNumber);
                    Point position = new Point(parseCoordinate(tokens[1], lineNumber),
                            parseCoordinate(tokens[2], lineNumber));
                    Piece piece = new Piece(parseEnum(PieceColor.class, tokens[3], lineNumber),
                            parseEnum(PieceType.class, tokens[4], lineNumber));
                    if (pieces.putIfAbsent(position, piece) != null) {
                        throw new InvalidSaveFileException(lineNumber, "deux pièces sur la même case");
                    }
                }
                default -> throw new InvalidSaveFileException(lineNumber, "instruction inconnue « " + tokens[0] + " »");
            }
        }

        if (!headerRead) {
            throw new InvalidSaveFileException("Fichier vide");
        }
        if (turn == null || !aiRead) {
            throw new InvalidSaveFileException("Sauvegarde incomplète : joueur actif ou IA manquant");
        }
        return new SavedGame(pieces, turn, aiColor, difficulty);
    }

    private static void expectTokens(String[] tokens, int expected, int lineNumber) throws InvalidSaveFileException {
        if (tokens.length != expected) {
            throw new InvalidSaveFileException(lineNumber, expected + " éléments attendus, " + tokens.length + " trouvés");
        }
    }

    private static int parseCoordinate(String token, int lineNumber) throws InvalidSaveFileException {
        try {
            int value = Integer.parseInt(token);
            if (value < 0 || value >= GameConfig.getBoardSize()) {
                throw new InvalidSaveFileException(lineNumber, "case hors du plateau : " + value);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidSaveFileException(lineNumber, "coordonnée invalide « " + token + " »");
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String token, int lineNumber)
            throws InvalidSaveFileException {
        try {
            return Enum.valueOf(type, token);
        } catch (IllegalArgumentException e) {
            throw new InvalidSaveFileException(lineNumber, "valeur inconnue « " + token + " »");
        }
    }
}
