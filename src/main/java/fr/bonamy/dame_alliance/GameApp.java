package fr.bonamy.dame_alliance;

import fr.bonamy.dame_alliance.controller.GameController;
import fr.bonamy.dame_alliance.model.AllianceMoveValidator;
import fr.bonamy.dame_alliance.model.Game;
import fr.bonamy.dame_alliance.view.BoardView;
import fr.bonamy.dame_alliance.view.GameView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class GameApp extends Application {

    private static final String STYLESHEET_PATH = "/css/style.css";

    @Override
    public void start(Stage stage) {
        Game game = new Game(new AllianceMoveValidator());
        GameView gameView = new GameView(new BoardView());
        new GameController(game, gameView);
        Scene scene = new Scene(gameView);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(STYLESHEET_PATH)).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Dame Alliance");
        stage.show();
        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
    }

    public static void main(String[] args) {
        launch();
    }

}
