package tasks;

import Exceptions.RepoXmlNotValidException;
import Exceptions.RepositoryAlreadyExistException;
import components.mainWindow.UIadapter;
import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;

import java.io.IOException;

import static moduleClasses.MainEngine.initRepo;

public class InitRepoTask extends Task {

    private String path;
    private String name;
    private UIadapter uIadapter;

    public InitRepoTask(String dirPath, String repoName, UIadapter uIadapter){
        this.name= repoName;
        this.path= dirPath;
        this.uIadapter=uIadapter;

    }
    @Override
    protected Object call() {
        try {
            if (FileUtils.getFile(path).exists())
                throw new RepositoryAlreadyExistException(path);

            else{
                initRepo(path,name);
            uIadapter.displayError("A repository by the name " +name +" has been created in "+path);
            }

        } catch (IOException e) {
            uIadapter.displayError(e.getMessage());

        } catch (RepoXmlNotValidException e) {
            uIadapter.displayError(e.getMessage());

        } catch (RepositoryAlreadyExistException e) {
            uIadapter.displayError(e.getMessage());
        }
        return null;
    }
}
