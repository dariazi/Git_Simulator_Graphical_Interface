package components.mainWindow;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TreeItem;
import moduleClasses.CommitObj;
import moduleClasses.ConflictObj;
import moduleClasses.RepoContainer;

import java.util.Map;
import java.util.function.Consumer;

public class UIadapter {
    Consumer<SimpleBooleanProperty> promptUser;
    Consumer<String>addNewBranch;
    Consumer<TreeItem> addTreeView;
    Consumer<RepoContainer> setRepo;
    Consumer<String> displayErrorMessage;
    Consumer<CommitObj> finalizeCommit;
    Consumer<String> displayFileContent;
    Consumer<Map<String, ConflictObj>> resolveConflicts;
    Runnable addCommitMap;

    public UIadapter(Consumer<String> branchConsumer, Consumer<TreeItem> treeViewConsumer, Consumer<RepoContainer> repoConsumer, Consumer<String> displayErrorMessage, Consumer<CommitObj> finalizeCommit ,
                     Consumer<String> displayFileContent, Consumer<Map<String, ConflictObj>> c, Consumer<SimpleBooleanProperty> p, Runnable commitMap){
        this.addNewBranch=branchConsumer;
        this.addTreeView=treeViewConsumer;
        this.setRepo= repoConsumer;
        this.displayErrorMessage= displayErrorMessage;
        this.finalizeCommit= finalizeCommit;
        this.displayFileContent=displayFileContent;
        this.resolveConflicts=c;
        this.promptUser= p;
        this.addCommitMap= commitMap;
    }
    public void alert(SimpleBooleanProperty b){
        promptUser.accept(b);
    }
    public UIadapter(Consumer <String> displayFileContent, Consumer<TreeItem> displayCommitTreeView){
        this.displayFileContent=displayFileContent;
        this.addTreeView=displayCommitTreeView;
    }
    public void addBranches(RepoContainer repo) {
               repo.getBranches().forEach((i, s)->
               {
                   if(!i.equals(repo.getHeadBranch()))
                   addNewBranch.accept(i);
               });
    }
      public void switchRepo(RepoContainer repo){
        setRepo.accept(repo);
    }

    public void displayError(String errorMessage){
        displayErrorMessage.accept(errorMessage);
    }

    public void finalizeCommit(CommitObj obj){
        finalizeCommit.accept(obj);
    }
    public void newTreeView(TreeItem t){ addTreeView.accept(t); }

    public void displayCommitContent(String i) {
        displayFileContent.accept(i);
    }
    public void createCommitMap(){
        this.addCommitMap.run();
    }
    public void displayConflicts(Map<String, ConflictObj> conflicts) {
        resolveConflicts.accept(conflicts);
    }

}
