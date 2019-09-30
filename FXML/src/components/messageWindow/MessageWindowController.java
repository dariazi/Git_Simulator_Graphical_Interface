package components.messageWindow;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;


public class MessageWindowController {

    @FXML
    Label messageLabel;
    @FXML
    Button exitBtn;
    @FXML
    Button confirmButton;

    private SimpleStringProperty errorMessage;
    private Stage returnStage;
    private SimpleBooleanProperty accept;

    public MessageWindowController() {
        this.errorMessage = new SimpleStringProperty("");
        this.accept=new SimpleBooleanProperty(false);

    }

    public void setMessage(String message) {
        this.errorMessage.set(message);

    }

    public void setReturnStage(Stage stage){
        returnStage=stage;
    }

    public void bindAcceptButton(SimpleBooleanProperty userInput){
        userInput.bind(this.accept);
    }

    @FXML
    private void initialize() {
        messageLabel.textProperty().bind(errorMessage);
    }

    @FXML
    private void exitToPrimaryStage(){
        returnStage.close();
    }
    @FXML
    private void acceptAction(){
        this.accept.setValue(true);
        returnStage.close();

    }



}
