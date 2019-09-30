package tasks;

import components.mainWindow.UIadapter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import moduleClasses.CommitObj;
import moduleClasses.FolderItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static moduleClasses.MainEngine.*;

//This class handles all differences calculations between repositories.
//The calling function passes this task the comaprison parameters, the comparison method, and what to do with the result
//for instance in viewOpenChangesAction onFinish is displayOpenChanges
//whereas in commitAction onFinish is finalizeCommit
public class WorkspaceUpdateTask extends Task {

    UIadapter uiAdapter;
    Consumer <CommitObj> onFinish;
    String currentRepo;
    Consumer<TreeItem> updateWC;
    public WorkspaceUpdateTask(Consumer<CommitObj> func, String repo, Consumer<TreeItem> updater, UIadapter uiAdapter){
        this.updateWC= updater;
        this.onFinish= func;
        this.currentRepo=repo;
        this.uiAdapter= uiAdapter;
    }

    @Override
    protected Boolean call() throws Exception {
        CommitObj obj= new CommitObj();
        Map<String, List<FolderItem>> mapOfdif=new HashMap<>();
        Map<String, List<FolderItem>> mapOfWC=new HashMap<>();
        checkForChanges(mapOfdif, obj, this.currentRepo);
        onFinish.accept(obj);
        String sha1= scanWorkingCopy(this.currentRepo, mapOfWC );
        updateWC.accept(createDirectoryTreeView(this.currentRepo,mapOfWC,sha1,i->uiAdapter.displayCommitContent(i) ));
        Platform.runLater(()->uiAdapter.createCommitMap());
        return null;
    }
}
