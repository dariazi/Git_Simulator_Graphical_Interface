package components.choiceWindowComponent;

import components.mainWindow.UIadapter;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import moduleClasses.MainEngine;

import java.io.File;


public class PopupController {

    @FXML
    TextField locationInput;
    @FXML
    TextField dirNameInput;
    @FXML
    TextField repoNameInput;
    @FXML
    Button chooseLocation;
    @FXML
    Button cancelBtn;
    @FXML
    Button acceptBtn;
    private MainEngine mainEngine;
    private Stage primaryStage;
    private UIadapter uIadapter;
    public void setPrimaryStage(Stage i_primaryStage) {
        primaryStage = i_primaryStage;
    }
    public void setMainEngine(MainEngine mainEngine){
        this.mainEngine = mainEngine;
    }
    public void setUIadapter(UIadapter uIadapter){
        this.uIadapter=uIadapter;
    }

    @FXML
    private void initialize() {
        BooleanBinding booleanBinding =
                locationInput.textProperty().isEmpty().or(
                        dirNameInput.textProperty().isEmpty().or(
                                repoNameInput.textProperty().isEmpty()));
        acceptBtn.disableProperty().bind(booleanBinding);
    }



    @FXML
    public void browseAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select directory");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        String filePath = selectedDirectory.getAbsolutePath();
        if (selectedDirectory == null) {
            return;
        }
        this.locationInput.setText(filePath);

    }

    @FXML
    public void cancelAction() {

        primaryStage.close();
    }

    @FXML
    public void acceptAction(){
        mainEngine.initNewRepo( this.locationInput.getText()+this.dirNameInput.getText(), this.repoNameInput.getText(),uIadapter);
        primaryStage.close();
    }


}



