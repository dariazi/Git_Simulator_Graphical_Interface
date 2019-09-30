package components.userMessageGetterComponent;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class UserInputGetter {

    @FXML
    TextArea messageGetter;
    @FXML
    Button confirmBtn;
    @FXML
    Label titleLabel;
    private Consumer<String> onFinish;
    private Stage myStage;
    public void setOnFinish(Consumer <String> onFinish){
        this.onFinish=onFinish;
    }
    public void setTitle(String title){
        this.titleLabel.setText(title);
    }
    public void setStage(Stage thisStage){
        this.myStage=thisStage;
    }

    @FXML
    private void confirmAction(){

        onFinish.accept(messageGetter.getText());
        this.myStage.close();
    }
}
