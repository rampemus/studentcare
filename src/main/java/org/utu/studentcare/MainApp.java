package org.utu.studentcare;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.utu.studentcare.applogic.DBApp;
import org.utu.studentcare.javafx.FXConsole;

public class MainApp extends Application {
    // https://openjfx.io/javadoc/11/javafx.graphics/javafx/application/Application.html

    // The JavaFX runtime does the following, in order, whenever an application is launched:

    // 1. Starts the JavaFX runtime, if not already started (see Platform.startup(Runnable) for more information)
    // 2. Constructs an instance of the specified Application class
    // 3. Calls the init() method
    // 4. Calls the start(javafx.stage.Stage) method
    // 5. Waits for the application to finish, which happens when either of the following occur:
    //   a) the application calls Platform.exit()
    //   b) the last window has been closed and the implicitExit attribute on Platform is true
    // 6. Calls the stop() method

    @Override
    public void init() { /* ei toiminnallisuutta tässä */ }

    @Override
    public void stop() { /* ei toiminnallisuutta tässä */ }

    /**
     * Creates a new text mode user interface for the application.
     * @param stage Main window
     *
     * TODO: tämä kaikki halutaan korvata graafisella käyttöliittymällä!
     */
    private void startTextModeUI(Stage stage) {
        FXConsole console = new FXConsole();

        stage.setWidth(1200);
        stage.setHeight(700);
        stage.show();
        stage.setScene(new Scene(new BorderPane(console) {{
            setBottom(console.inputField);
        }}));

        DBApp.init("value4life.db", s -> { Platform.runLater(stage::close); console.close(); s.accept(null); Platform.exit(); }, console.commands);

    }

    /**
     * Creates a new graphical user interface for the application.
     * @param stage Main window
     */
    private void startGraphicalUI(Stage stage) {
        // TODO: toteuta!
    }

    @Override
    public void start(Stage stage) {
        // TODO: vaihda kun teet graafista käyttöliittymää
        boolean textMode = true;

        if (textMode)
            startTextModeUI(stage);
        else
            startGraphicalUI(stage);
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
