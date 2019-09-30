package components.commitTree.commitNode;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.shape.Circle;
import moduleClasses.CommitObj;

import java.util.List;
import java.util.function.Consumer;

public class CommitNodeController {

    //    @FXML
//    private Label commitTimeStampLabel;
//    @FXML
//    private Label messageLabel;
//    @FXML
//    private Label committerLabel;
    @FXML
    private Circle CommitCircle;
    @FXML
    private Label branchesLabel;

    private Consumer<CommitObj> OnClick;
    private Consumer<TreeItem> displayContents;
    private CommitObj m_CommitDetails;
    private TreeItem treeView;

    public int getCircleRadius() {
        return (int) CommitCircle.getRadius();
    }

    public void setBranches(List<String> i_Branches) {
        if (!i_Branches.isEmpty()) {
            branchesLabel.setText("<- " + String.join(",", i_Branches));
        }
    }

    public void commitRow_OnCLick() {
        OnClick.accept(m_CommitDetails);
        displayContents.accept(treeView);

    }

    public void setCommitDetails(CommitObj i_commitDetails) {
        m_CommitDetails = i_commitDetails;
    }

    public void setOnClick(Consumer<CommitObj> i_Consumer) {
        OnClick = i_Consumer;
    }


    public void setTreeView(TreeItem root){
        this.treeView= root;
    }
    public void setDisplay(Consumer<TreeItem> view){
        displayContents= view;
    }
}
