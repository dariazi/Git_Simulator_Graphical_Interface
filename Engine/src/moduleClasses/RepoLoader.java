package moduleClasses;

import Exceptions.FolderIsNotEmptyException;
import Exceptions.RepoXmlNotValidException;
import Exceptions.RepositoryAlreadyExistException;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static common.MagitResourcesConstants.HEAD_RELATIVE_PATH;
import static moduleClasses.MainEngine.listAllBranches;
import static moduleClasses.MainEngine.scanWorkingCopy;

public class RepoLoader implements NewRepoLoader {
    private String address;
    public RepoLoader(String addr){
        this.address= addr;
    }
    @Override
    public  RepoContainer createRepoContainer() throws RepositoryAlreadyExistException, IOException, FolderIsNotEmptyException, RepoXmlNotValidException {
        String head =EngineUtils.readFileToString(address+HEAD_RELATIVE_PATH);
        Map<String,String> branches= listAllBranches(address);
        Map <String, List<FolderItem>> map= new HashMap<>();
        String rootDirSHA1 = scanWorkingCopy(address,map);
        String name= EngineUtils.readFileToString(address+"\\.magit\\name");

        RepoContainer repo= new RepoContainer(head,map,branches,rootDirSHA1, address,name );
        if(FileUtils.getFile(address+"\\.magit\\remote").exists()) {
            String [] remote= EngineUtils.readFileToString(address+"\\.magit\\remote").split("\n");
            repo.setRemoteRef(remote[0],remote[1]);
        }
        return repo;

    }
}
