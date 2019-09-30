package components.Merger;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class MergeController {
    @FXML
    private Text ourFile;
    @FXML
    private Text theirFile;
    @FXML
    private Text parentFile;
    @FXML
    private TextArea mergeProduct;
    @FXML
    private Button applyBtn;
    private Stage stage;
    private Consumer<String> passMerge;
    public MergeController(){

    }
    public void setOurs(String content){
        ourFile.setText(content);
    }
    public void setTheirs(String content){
        theirFile.setText(content);
    }
    public void setParent(String content){
        parentFile.setText(content);
    }
    public void setMerger(Consumer<String> content){
        passMerge= content;
    }
    @FXML
    public void applyAction(){
        passMerge.accept(mergeProduct.getText());
        stage.close();
    }
    public void setStage(Stage s){
        stage=s;
    }


}
