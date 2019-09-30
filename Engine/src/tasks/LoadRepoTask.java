package tasks;

import components.mainWindow.UIadapter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import moduleClasses.NewRepoLoader;
import moduleClasses.RepoContainer;

import static moduleClasses.MainEngine.createDirectoryTreeView;

public class LoadRepoTask extends Task {

    private String fileName;
    private UIadapter uiadapter;
    private NewRepoLoader loaderType;


    public LoadRepoTask(String i_fileName, UIadapter i_uiadapter, NewRepoLoader loader){
        this.fileName= i_fileName;
        this.uiadapter= i_uiadapter;
        this.loaderType=loader;

    }

    @Override
    protected Boolean call() throws Exception {
//TODO: add progress bar!
        RepoContainer currRepo=loaderType.createRepoContainer();

        TreeItem directoryRoot=createDirectoryTreeView(currRepo.getDirPath(),currRepo.getWC(),currRepo.getRootDir(),(i)->System.out.println(i+"clicked")) ;


        Platform.runLater(()->{
                    this.uiadapter.switchRepo(currRepo);
                    this.uiadapter.addBranches(currRepo);
                    this.uiadapter.newTreeView(directoryRoot);
                  //  this.uiadapter.createCommitMap();
        }
        );

        return null;
    }



}
