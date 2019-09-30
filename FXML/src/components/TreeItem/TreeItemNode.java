package components.TreeItem;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.function.Consumer;

public class TreeItemNode {

    @FXML
    Label fileName;
    private String content;
    private Consumer<String> displayContent;

    public void setDisplayContent(Consumer<String> consumer) {
        displayContent = consumer;
    }

    public void setName(String name) {
        this.fileName.setText(name);
    }

    public void setContent(String content) {
        this.content = content;
    }

    @FXML
    public void displayFileContent() {
        displayContent.accept(content);
    }
}
