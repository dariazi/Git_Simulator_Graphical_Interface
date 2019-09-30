package moduleClasses;

import Exceptions.FolderIsNotEmptyException;
import Exceptions.RepoXmlNotValidException;
import Exceptions.RepositoryAlreadyExistException;
import jaxbClasses.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static common.MagitResourcesConstants.*;
import static moduleClasses.MainEngine.*;

public class XMLloader implements NewRepoLoader {
    public String XMLaddress;

    public XMLloader(String address){
        this.XMLaddress= address;
    }


    @Override
    public  RepoContainer createRepoContainer() throws RepositoryAlreadyExistException, IOException, FolderIsNotEmptyException, RepoXmlNotValidException {
        MagitRepository repoToParse  = JAXBHandler.loadXML(XMLaddress);
        Map<String, MagitBlob> repoBlobs = EngineUtils.createRepoBlobsMap(repoToParse.getMagitBlobs());
        Map<String, MagitSingleFolder> repoFolders = EngineUtils.createRepoFoldersMaps(repoToParse.getMagitFolders(), repoBlobs);
        Map<String, CommitObj> commitObjectsMap = EngineUtils.createRepoCommitMap(repoToParse.getMagitCommits(), repoFolders);// map where the key is id and the values are commit objects
        Map<String, String> branchesMap = EngineUtils.createRepoBranchMap(repoToParse.getMagitBranches(), commitObjectsMap.keySet());
        EngineUtils.isHEADValid(repoToParse, branchesMap.keySet());
        String fullPath = repoToParse.getLocation();
        Boolean isRemote= false;
        String remoteLocation= new String();
        String remoteName=new String();
        initRepo(fullPath, repoToParse.getName());
        if(repoToParse.getMagitRemoteReference().getLocation()!=null ){
             remoteLocation= repoToParse.getMagitRemoteReference().getLocation();
             remoteName = repoToParse.getMagitRemoteReference().getName();
            FileUtils.forceMkdir(new File(fullPath + BRANCHES + remoteName));
            isRemote=true;

        }

        List<MagitSingleCommit> firstCommit = repoToParse.getMagitCommits().getMagitSingleCommit().stream()
                .filter(magitSingleCommit -> magitSingleCommit.getPrecedingCommits() == null
                        || magitSingleCommit.getPrecedingCommits().getPrecedingCommit().size() == 0)
                .collect(Collectors.toList());


        createRepositoryRec(firstCommit.get(0).getId(), "", repoFolders, repoBlobs, commitObjectsMap, branchesMap, repoToParse.getLocation());
        EngineUtils.overWriteFileContent(fullPath + HEAD_RELATIVE_PATH, repoToParse.getMagitBranches().getHead());
        String rootSha1=EngineUtils.getLastCommitRoot(fullPath);
        Map<String, List<FolderItem>> map = createLatestCommitMap(rootSha1, fullPath);
        parseMapToWC(map, rootSha1, fullPath + OBJECTS, fullPath);
        RepoContainer repo= new RepoContainer(repoToParse.getMagitBranches().getHead(), map, branchesMap,rootSha1, repoToParse.getLocation(),repoToParse.getName() );
        if(isRemote==true ){
            repo.setRemoteRef(remoteLocation, remoteName);
            EngineUtils.stringToTextFile(fullPath + "\\.magit\\", remoteLocation + "\n" + remoteName, "remote");
        }
        return repo;
    }


    public void createRepositoryRec(String currCommitID,
                                           String prevCommitSha1,
                                           Map<String, MagitSingleFolder> repoFolders,
                                           Map<String, MagitBlob> repoBlobs,
                                           Map<String, CommitObj> commitObjectsMap,
                                           Map<String, String> branchesMap,
                                           String repo) {

        CommitObj currCommit = commitObjectsMap.get(currCommitID);
        if (currCommit.rootDirSha1==null)
        currCommit.rootDirSha1 = magitSingleCommitToMap(currCommit.rootFolderID, repoFolders, repoBlobs, repo);
        currCommit.setPrevSha1(prevCommitSha1);
        String newCommitContent = currCommit.toString();
        String Sha1 = DigestUtils.sha1Hex(newCommitContent);

        try {
            if(currCommit.getSecondPrecedingSha1().equals("")&& currCommit.PreviousCommitsID.get(1).equals("")
            || !currCommit.getSecondPrecedingSha1().equals("")&& !currCommit.PreviousCommitsID.get(1).equals("")) {
                EngineUtils.StringToZipFile(newCommitContent, repo + OBJECTS, Sha1);
                branchesMap.forEach((name, cID) -> {
                    if (cID.equals(currCommitID))
                        EngineUtils.stringToTextFile(repo + BRANCHES, Sha1, name);

                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        commitObjectsMap.forEach((key, commitObj) ->
        {
            if (commitObj.PreviousCommitsID.contains(currCommitID))
                createRepositoryRec(key, Sha1, repoFolders, repoBlobs, commitObjectsMap, branchesMap, repo);

        });


    }


    public String magitSingleCommitToMap(String i_parentFolderID, Map<String, MagitSingleFolder> repoFolders, Map<String, MagitBlob> repoBlobs, String path) {
        MagitSingleFolder rootFolder = repoFolders.get(i_parentFolderID);
        List<Item> folderItems = rootFolder.getItems().getItem();
        List<FolderItem> folderContents = new LinkedList<>();
        String folderSha1 = null;
        String itemSha1;
        try {
            for (Item folderItem : folderItems) {
                String itemType = folderItem.getType();
                String itemID = folderItem.getId();
                if (itemType.equals("blob")) {
                    MagitBlob blob = repoBlobs.get(itemID);
                    String content = blob.getContent().replaceAll("\\r", "");
                    itemSha1 = DigestUtils.sha1Hex(content);
                    FolderItem blobItem = new FolderItem(itemSha1, blob.getName(), blob.getLastUpdater(), blob.getLastUpdateDate(), "file");
                    EngineUtils.StringToZipFile(content, path + OBJECTS, itemSha1);

                    folderContents.add(blobItem);

                } else if (itemType.equals("folder")) {

                    itemSha1 = magitSingleCommitToMap(itemID, repoFolders, repoBlobs, path);
                    MagitSingleFolder folder = repoFolders.get(itemID);
                    FolderItem item = new FolderItem(itemSha1, folder.getName(), folder.getLastUpdater(), folder.getLastUpdateDate(), "folder");
                    folderContents.add(item);
                }
            }
            Collections.sort(folderContents, FolderItem::compareTo);
            folderSha1 = calculateFileSHA1(folderContents);
            EngineUtils.StringToZipFile(EngineUtils.listToString(folderContents, "\n"), path + OBJECTS, folderSha1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return folderSha1;

    }

}
