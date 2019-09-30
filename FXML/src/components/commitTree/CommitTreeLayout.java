package components.commitTree;

import com.fxgraph.graph.Graph;
import com.fxgraph.graph.ICell;
import com.fxgraph.layout.Layout;
import components.commitTree.commitNode.CommitNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// simple test for scattering commits in imaginary tree, where every 3rd node is in a new 'branch' (moved to the right)
public class CommitTreeLayout implements Layout {

    @Override
    public void execute(Graph graph) {
        List<CommitNode> cells = (List<CommitNode>) (List<?>) graph.getModel().getAllCells();
        cells.sort(CommitNode::compareTo);
        Map<String, Integer> locatedCells = new HashMap<>();
        locatedCells.put(cells.get(0).commitDetails.getCommitSha1(), 10);
        Integer startY = 50 * cells.size();
        Integer startX = 10;
        graph.getGraphic(cells.get(0)).relocate(startX, startY);
        startY -= 50;
        for (ICell cell : cells) {
            CommitNode commitNode = (CommitNode) cell;
            startX = locatedCells.get(commitNode.commitDetails.getFirstPrecedingSha1());
            if (startX != null) {
                locatedCells.replace(commitNode.commitDetails.getFirstPrecedingSha1(), startX + 50);
                graph.getGraphic(commitNode).relocate(startX, startY);
                locatedCells.put(commitNode.commitDetails.getSha1(), startX);
                startY -= 50;
            }

        }
    }
}
