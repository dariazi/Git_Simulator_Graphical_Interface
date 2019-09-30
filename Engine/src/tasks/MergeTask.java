package tasks;

import components.mainWindow.UIadapter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import moduleClasses.*;
import org.apache.commons.io.FileUtils;
import puk.team.course.magit.ancestor.finder.AncestorFinder;
import puk.team.course.magit.ancestor.finder.CommitRepresentative;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static common.MagitResourcesConstants.BRANCHES;
import static moduleClasses.MainEngine.compareMaps;

public class MergeTask extends Task<Boolean> {

    private String ancestorSha1;
    private String oursSha1;
    private String theirsSha1;
    private String ourBranch;
    private String theirBranch;
    private Map<String, ConflictObj> conflicts; // the keys are file paths and the values are the conflict objects. After a conflict is
    // resolved we remove it from this map and add it to the merge product map.
    private Map<String, String> mergeProduct; //the keys are file paths and the values are fie contents
    private String repoPath;
    private Map<String, List<FolderItem>> ancestorMap;
    private CalcDiff oursVparent;
    private CalcDiff theirsVparent;
    private MainEngine engine;
    private String ancestorRootSha1;
    private UIadapter uiAdapter;
    private String relPath;

    public MergeTask(String ours, String theirs, String path, MainEngine engineObj, UIadapter uIadapter) {
        ourBranch = ours;
        theirBranch = theirs;
        repoPath = path;
        engine = engineObj;
        uiAdapter = uIadapter;
        oursVparent = new CalcDiff();
        theirsVparent = new CalcDiff();
        conflicts= new HashMap<>();
        mergeProduct=new HashMap<>();
        relPath=path + "\\.magit\\objects\\";

    }

    @Override
    protected Boolean call() throws Exception {
        getSha1();
        if(oursSha1.equals(ancestorSha1)){
            fastForewardMerge();
        }
        else if(theirsSha1.equals(ancestorSha1)){
           Platform.runLater( ()->uiAdapter.displayError("Nothing to merge!"));
        }
        else{
        mapDifferences();// create maps of sons and ancestors
        createRepo();
        Platform.runLater(()-> {resolveConflicts();
        createWCRepo();
        updateWCrepo();
                    try {
                        engine.promptCommit(repoPath,b->uiAdapter.alert(b),c-> uiAdapter.finalizeCommit(c),Optional.empty(), Optional.ofNullable(theirsSha1));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );}
        return true;
    }

    private void fastForewardMerge() {
        EngineUtils.overWriteFileContent(repoPath+BRANCHES+ourBranch,theirsSha1);
    }

    private void resolveConflicts() {
uiAdapter.displayConflicts(conflicts);

    }
    private void createWCRepo(){
        Map <String,String> resMap= new HashMap<>();
        mergeProduct.forEach((i,s)-> {
            try {
                resMap.put(i,EngineUtils.listToString(EngineUtils.getZippedFileLines(relPath+ s +".zip"),"\n"));

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        conflicts.forEach((i,s)->
                resMap.put(i, s.getContent()));
        this.mergeProduct= resMap;

    }

    private void createRepo() {
        createRepoRec(repoPath,ancestorRootSha1);
        resolveNewFiles();

    }
    private void resolveNewFiles(){
        oursVparent.added.forEach((i,s)-> {if(theirsVparent.added.containsKey(i))
        conflicts.put(i, new ConflictObj("",s, theirsVparent.added.get(i)));
        else mergeProduct.put(i,s);
        });
        theirsVparent.added.forEach((i,s)-> {
            mergeProduct.put(i,s);
        });



    }
    private void updateWCrepo(){
        //TODO: add user prompt if both conflicts and mergeproduct are empty
        File rootDirFileObj = FileUtils.getFile(repoPath);
        if (rootDirFileObj.exists()) {
            File[] dirItems = rootDirFileObj.listFiles();
            if (dirItems.length > 0) {
                for (File file : dirItems
                ) {
                    if(!file.getName().contains(".magit"))
                    FileUtils.deleteQuietly(file);
                }
            }
        }
        mergeProduct.forEach((i,s)->{
            File file = new File(i);
            file.getParentFile().mkdirs();
            EngineUtils.stringToTextFile(i,s,"");});
    }

    private void createRepoRec(String path, String folderSha1) {
        String filePath;
        for (FolderItem i : ancestorMap.get(folderSha1)){
            if (i.getType().equals("folder"))
                createRepoRec(path + "\\" + i.getItemName(), i.getSha1());
            else if (i.getType().equals("file")) {
                filePath = path + "\\" + i.getItemName();
                checkForConflict(filePath, i.getSha1());
            }
            }

    }


    private void checkForConflict(String path, String ancestorsha1) {
        String oursFileSha1 = oursVparent.deleted.containsKey(path) ? "" : (oursVparent.changed.containsKey(path) ? oursVparent.changed.get(path) : "");
        String theirsFileSha1 = theirsVparent.deleted.containsKey(path) ? "" : (theirsVparent.changed.containsKey(path) ? theirsVparent.changed.get(path) : "");
        if (!oursFileSha1.equals("") || !theirsFileSha1.equals(""))
            conflicts.put(path, new ConflictObj(ancestorsha1, oursFileSha1, theirsFileSha1));
    }


    private void mapDifferences() throws IOException {
        Map<String, List<FolderItem>> mapOfOurs = new HashMap<>();
        Map<String, List<FolderItem>> mapOfTheirs;
        String oursRootSha1 = engine.scanWorkingCopy(repoPath, mapOfOurs);
        String theirsRootSha1 = EngineUtils.getZippedFileLines(relPath + theirsSha1 + ".zip").get(0);
        ancestorRootSha1 = EngineUtils.getZippedFileLines(relPath + ancestorSha1 + ".zip").get(0);
        mapOfTheirs = MainEngine.createLatestCommitMap(theirsRootSha1, repoPath);
        ancestorMap = MainEngine.createLatestCommitMap(ancestorRootSha1, repoPath);
        compareMaps(mapOfOurs, ancestorMap, oursRootSha1, ancestorRootSha1, repoPath, oursVparent);
        compareMaps(mapOfTheirs, ancestorMap, theirsRootSha1, ancestorRootSha1, repoPath, theirsVparent);

    }

    private void getSha1() {
        oursSha1 = EngineUtils.readFileToString(repoPath + BRANCHES + ourBranch);
        theirsSha1 = EngineUtils.readFileToString(repoPath + BRANCHES + theirBranch);
        AncestorFinder ancestorFinder = new AncestorFinder(sha1 -> this.mapPrevCommitsRec(sha1));
        ancestorSha1 = ancestorFinder.traceAncestor(oursSha1, theirsSha1);
    }

    private CommitRepresentative mapPrevCommitsRec(String commitSha1) {
        try {
            CommitObj commit = new CommitObj();
            List<String> commitFields = EngineUtils.getZippedFileLines(relPath + commitSha1 + ".zip");
            commit.setSha1(commitSha1);
            String[] prevCommitsSha1 = {"",""};
            prevCommitsSha1 =commitFields.get(1).split(",");
            commit.setPreviousCommitsSha1(prevCommitsSha1);
            return commit;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
