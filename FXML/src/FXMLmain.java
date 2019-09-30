import components.mainWindow.MainWindowController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import moduleClasses.MainEngine;
import org.fxmisc.cssfx.CSSFX;

import java.net.URL;

    public class FXMLmain extends Application {


        @Override
        public void start(Stage primaryStage) throws Exception {

            CSSFX.start();

            FXMLLoader loader = new FXMLLoader();

            URL mainFXML = getClass().getResource("/components/mainWindow/mainComponentFXML.fxml");
            loader.setLocation(mainFXML);
            BorderPane root = loader.load();

            MainWindowController mainController = loader.getController();
            MainEngine engine = new MainEngine(mainController);
            mainController.setPrimaryStage(primaryStage);
            mainController.setEngine(engine);

            primaryStage.setTitle("MAgit");
            Scene scene = new Scene(root, 1200, 675);


            // first try implementing commit tree

            primaryStage.setScene(scene);
            primaryStage.show();




        }

        public static void main(String[] args) {

            launch(args);
        }
    }


