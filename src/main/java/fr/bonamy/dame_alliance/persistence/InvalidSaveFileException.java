package fr.bonamy.dame_alliance.persistence;

import java.io.IOException;

public class InvalidSaveFileException extends IOException {

    public InvalidSaveFileException(String message) {
        super(message);
    }

    public InvalidSaveFileException(int lineNumber, String message) {
        super("Ligne " + lineNumber + " : " + message);
    }
}
