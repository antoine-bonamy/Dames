package fr.bonamy.dame_alliance.persistence;

import fr.bonamy.dame_alliance.model.Piece;
import fr.bonamy.dame_alliance.model.PieceColor;
import fr.bonamy.dame_alliance.model.PieceType;
import fr.bonamy.dame_alliance.model.ai.Difficulty;
import fr.bonamy.dame_alliance.util.Point;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameFileStoreTest {

    private final GameFileStore store = new GameFileStore();

    @TempDir
    Path tempDir;

    @Test
    void savedGameCanBeLoadedBack() throws IOException {
        SavedGame saved = new SavedGame(Map.of(
                new Point(1, 2), new Piece(PieceColor.WHITE, PieceType.DIAGONAL),
                new Point(4, 5), new Piece(PieceColor.BLACK, PieceType.KING)),
                PieceColor.BLACK, PieceColor.WHITE, Difficulty.HARD);
        Path file = tempDir.resolve("partie.dalli");

        store.save(saved, file);

        assertEquals(saved, store.load(file));
    }

    @Test
    void twoPlayerGameHasNoAi() throws IOException {
        SavedGame saved = new SavedGame(Map.of(new Point(0, 0), new Piece(PieceColor.WHITE, PieceType.KING)),
                PieceColor.WHITE, null, null);
        Path file = tempDir.resolve("deux-joueurs.dalli");

        store.save(saved, file);
        SavedGame loaded = store.load(file);

        assertNull(loaded.aiColor());
        assertNull(loaded.difficulty());
    }

    @Test
    void fileIsHumanReadable() throws IOException {
        Path file = tempDir.resolve("lisible.dalli");
        store.save(new SavedGame(Map.of(new Point(3, 2), new Piece(PieceColor.WHITE, PieceType.ORTHOGONAL)),
                PieceColor.WHITE, PieceColor.BLACK, Difficulty.MEDIUM), file);

        assertEquals("""
                dame-alliance 1
                turn WHITE
                ai BLACK MEDIUM
                piece 3 2 WHITE ORTHOGONAL
                """, Files.readString(file));
    }

    @Test
    void rejectsFileWithoutHeader() throws IOException {
        Path file = write("turn WHITE\nai NONE\n");

        InvalidSaveFileException error = assertThrows(InvalidSaveFileException.class, () -> store.load(file));
        assertTrue(error.getMessage().startsWith("Ligne 1"));
    }

    @Test
    void rejectsPieceOutsideBoard() throws IOException {
        Path file = write("dame-alliance 1\nturn WHITE\nai NONE\npiece 8 0 WHITE KING\n");

        InvalidSaveFileException error = assertThrows(InvalidSaveFileException.class, () -> store.load(file));
        assertTrue(error.getMessage().startsWith("Ligne 4"));
    }

    @Test
    void rejectsTwoPiecesOnSameTile() throws IOException {
        Path file = write("dame-alliance 1\nturn WHITE\nai NONE\npiece 1 1 WHITE KING\npiece 1 1 BLACK KING\n");

        assertThrows(InvalidSaveFileException.class, () -> store.load(file));
    }

    @Test
    void rejectsUnknownValues() throws IOException {
        Path file = write("dame-alliance 1\nturn RED\nai NONE\n");

        assertThrows(InvalidSaveFileException.class, () -> store.load(file));
    }

    @Test
    void rejectsIncompleteFile() throws IOException {
        Path file = write("dame-alliance 1\npiece 1 1 WHITE KING\n");

        assertThrows(InvalidSaveFileException.class, () -> store.load(file));
    }

    private Path write(String content) throws IOException {
        Path file = tempDir.resolve("test.dalli");
        Files.writeString(file, content);
        return file;
    }
}
