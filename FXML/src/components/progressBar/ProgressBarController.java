//package components.progressBar;
//
//import javafx.beans.binding.Bindings;
//import javafx.beans.property.SimpleIntegerProperty;
//import javafx.concurrent.Task;
//import javafx.fxml.FXML;
//import javafx.scene.control.Label;
//import javafx.scene.control.ProgressBar;
//
//public class ProgressBarController {
//
//    @FXML
//    ProgressBar progressBar;
//    @FXML
//    Label filesLoadedLabel;
//    private SimpleIntegerProperty filesLoaded;
//
//    ProgressBarController(Task<Boolean> aTask,){
//        this.filesLoaded= filesLoaded;
//
//    }
//    @FXML
//    private void initialize(){
//        progressBar.progressProperty().bind(this.filesLoaded);
//        filesLoadedLabel.textProperty().bind(Bindings.concat("Files loaded:", this.filesLoaded));
//    }
//}
