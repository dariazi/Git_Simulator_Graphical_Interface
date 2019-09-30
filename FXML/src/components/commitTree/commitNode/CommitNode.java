package components.commitTree.commitNode;

import com.fxgraph.cells.AbstractCell;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.IEdge;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import moduleClasses.CommitObj;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.function.Consumer;

public class CommitNode extends AbstractCell {

    public CommitObj commitDetails;
    private FXMLLoader fxmlLoader;
    private CommitNodeController commitNodeController;
    private GridPane root;

    public CommitNode(CommitObj i_commit, Consumer<CommitObj> i_onClick, TreeItem treeview, Consumer<TreeItem> displayContents) {
        try {
            fxmlLoader = new FXMLLoader();
            URL url = getClass().getResource("commitNode.fxml");
            fxmlLoader.setLocation(url);
            root = fxmlLoader.load(url.openStream());
        } catch (IOException e) {
            return;
        }
        commitDetails = new CommitObj(i_commit.dateCreated, i_commit.commitMessage, i_commit.getUserName(), i_commit.rootDirSha1, i_commit.PreviousCommitsSha1, i_commit.getCommitSha1(), i_commit.getBranchesPointToThisCommit());
        commitNodeController = fxmlLoader.getController();
        commitNodeController.setOnClick(i_onClick);
        commitNodeController.setDisplay(displayContents);
        commitNodeController.setTreeView(treeview);

    }

    public int compareTo(Object i_commitNodeToCompare) {
        CommitNode commitNodeToCompare = (CommitNode) i_commitNodeToCompare;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:sss");
        try {
            //exception here
            Date parsedDate = dateFormat.parse(commitDetails.dateCreated);
            Timestamp thisNodeTS = new Timestamp(parsedDate.getTime());

            parsedDate = dateFormat.parse(commitNodeToCompare.commitDetails.dateCreated);
            Timestamp nodeToCompareTS = new Timestamp(parsedDate.getTime());
            return thisNodeTS.compareTo(nodeToCompareTS);
        } catch (ParseException e) {
            return 0;
        }

    }

    @Override
    public Region getGraphic(Graph graph) {


        commitNodeController.setCommitDetails(commitDetails);
//            commitNodeController.setCommitMessage(commitDetails.commitMessage);
//            commitNodeController.setCommitter(commitDetails.getUserName());
//            commitNodeController.setCommitTimeStamp(commitDetails.dateCreated);
        commitNodeController.setBranches(commitDetails.getBranchesPointToThisCommit());

        return root;

    }

    @Override
    public DoubleBinding getXAnchor(Graph graph, IEdge edge) {
        final Region graphic = graph.getGraphic(this);
        return graphic.layoutXProperty().add(commitNodeController.getCircleRadius());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommitNode that = (CommitNode) o;

        return Objects.equals(commitDetails.dateCreated, that.commitDetails.dateCreated);
    }

    @Override
    public int hashCode() {
        return commitDetails.dateCreated != null ? commitDetails.dateCreated.hashCode() : 0;
    }
}

