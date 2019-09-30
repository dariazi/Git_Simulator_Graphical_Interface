package components.branchButton;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;

import java.util.function.Consumer;

public class SingleBranchController{

    @FXML Label singleBranch;
    @FXML MenuItem deleteBranch;
    @FXML MenuItem rebaseBranch;
    @FXML MenuItem checkoutBranch;
    @FXML MenuItem mergeBranch;
    @FXML ContextMenu menu;

    private SimpleBooleanProperty isRemote;
    protected SimpleStringProperty name;
    private String fullName;
    public SingleBranchController() {
        this.name= new SimpleStringProperty("");
        this.isRemote = new SimpleBooleanProperty(false);
    }


    @FXML
    public void setWord(String word)
    {
        if(word.contains("/")) {word= word.split("/")[1];
        isRemote.setValue(true);
        this.singleBranch.setStyle("-fx-text-fill: grey");
        }
        this.name.set(word);
        this.singleBranch.setId(word);

    }

    @FXML
    private void initialize() {
        singleBranch.textProperty().bind(name);
        mergeBranch.disableProperty().bind(isRemote);
        rebaseBranch.disableProperty().bind(isRemote.not());
    }

    public void setDelete(Consumer<String> deleteEventHandler){
        this.deleteBranch.setOnAction(event -> deleteEventHandler.accept(name.getValue()));
    }
    public void setCheckout(Consumer<String> checkoutEventHandler){

        this.checkoutBranch.setOnAction(event -> checkoutEventHandler.accept(name.getValue()));
    }
    public void setMerge(Consumer<String> mergeEventHandler){
        this.mergeBranch.setOnAction(event -> mergeEventHandler.accept(name.getValue()));
    }
    public void setRebaseBranch(Consumer<String> rebaseEventHandler){
        String branch= new String(name.getValue());
        if(isRemote.getValue()==true)
        this.rebaseBranch.setOnAction(event -> rebaseEventHandler.accept("/"+branch));
        else this.rebaseBranch.setOnAction(event -> rebaseEventHandler.accept(branch));


    }
    public String getName(){
        return this.name.getValue();
    }


}
