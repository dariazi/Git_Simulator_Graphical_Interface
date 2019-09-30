package moduleClasses;

import Exceptions.FolderIsNotEmptyException;
import Exceptions.RepoXmlNotValidException;
import Exceptions.RepositoryAlreadyExistException;

import java.io.IOException;

public interface NewRepoLoader {
    public RepoContainer createRepoContainer() throws RepositoryAlreadyExistException, IOException, FolderIsNotEmptyException, RepoXmlNotValidException;

}
