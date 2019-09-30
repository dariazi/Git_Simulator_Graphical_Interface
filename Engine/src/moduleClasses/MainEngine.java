package moduleClasses;

import Exceptions.FolderIsNotEmptyException;
import Exceptions.RepoXmlNotValidException;
import Exceptions.RepositoryAlreadyExistException;
import com.fxgraph.graph.Graph;
import components.TreeItem.TreeItemNode;
import components.mainWindow.MainWindowController;
import components.mainWindow.UIadapter;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import jaxbClasses.MagitRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Equator;
import org.apache.commons.io.FileUtils;
import tasks.InitRepoTask;
import tasks.LoadRepoTask;
import tasks.MergeTask;
import tasks.WorkspaceUpdateTask;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;

import static common.MagitResourcesConstants.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class MainEngine {
    MainWindowController m_mainWindowController;
    private Task currentRunningTask;


    //Empty constructor for the console UI.
    public MainEngine() {
    }

    public MainEngine(MainWindowController i_mainWindowController) {
        this.m_mainWindowController = i_mainWindowController;

    }

    public static String scanWorkingCopy(String currentRepository1, Map<String, List<FolderItem>> foldersMap) throws IOException {


        Path dirPath = Paths.get(currentRepository1);
        File dir = dirPath.toFile();
        List<FolderItem> filesList = new LinkedList<>();
        walk(dir, foldersMap, filesList);
        String rootSha1 = calculateFileSHA1(filesList);
        foldersMap.put(rootSha1, filesList);
        return rootSha1;

    }

    public static void walk(File dir, Map<String, List<FolderItem>> foldersMap, List<FolderItem> parentFolder) throws IOException {
        String fileContent;
        Path path;
        BasicFileAttributes attr;
        List<FolderItem> subFiles;
        FolderItem currentFolderItem;
        for (final File folderItem : dir.listFiles()) {
            if (!folderItem.getName().endsWith(".magit")) {
                path = Paths.get(folderItem.getPath());
                attr = Files.readAttributes(path, BasicFileAttributes.class);

                if (folderItem.isDirectory()) {
                    if (folderItem.list().length == 0)
                        folderItem.delete();
                    else {
                        subFiles = new LinkedList<FolderItem>();
                        walk(folderItem, foldersMap, subFiles);
                        Collections.sort(subFiles, FolderItem::compareTo);
                        String key = calculateFileSHA1(subFiles);
                        foldersMap.put(key, subFiles);

                        currentFolderItem = new FolderItem(key, folderItem.getName(), "user name", attr.lastModifiedTime().toString(), "folder");
                        parentFolder.add(currentFolderItem);
                    }

                }

                if (folderItem.isFile()) {
                    fileContent = EngineUtils.readFileToString(folderItem.getPath());
                    currentFolderItem = new FolderItem(DigestUtils.sha1Hex(fileContent), folderItem.getName(), "user name", attr.lastModifiedTime().toString(), "file");
                    currentFolderItem.setFileContent(fileContent);
                    parentFolder.add(currentFolderItem);


                }

            }
        }
        return;

    }

    public static String calculateFileSHA1(List<FolderItem> folderContent) {
        String res = "";
        for (FolderItem fItem : folderContent) {
            res = res + fItem.getSha1();
        }
        return DigestUtils.sha1Hex(res);
    }

    public static void initRepo(String rootDirPath, String name) throws IOException, RepoXmlNotValidException {
        if (rootDirPath != null && name != null && name != "" && rootDirPath != "") {
            File rootDirFileObj = FileUtils.getFile(rootDirPath);
            if (rootDirFileObj.exists()) {
                File[] dirItems = rootDirFileObj.listFiles();
                if (dirItems.length > 0) {
                    for (File file : dirItems
                    ) {
                        FileUtils.deleteQuietly(file);
                    }
                }
            }
            FileUtils.forceMkdir(rootDirFileObj);
            Path objectsDirPath = java.nio.file.Paths.get(rootDirPath + OBJECTS);
            Files.createDirectories(objectsDirPath);
            File branchesFileObj = FileUtils.getFile(rootDirPath + BRANCHES);
            FileUtils.forceMkdir(branchesFileObj);
            String headContent = "master";
            String branchesPath = branchesFileObj.getAbsolutePath();
            File headFile = new File(branchesPath + "\\HEAD");
            File masterFile = new File(branchesPath + "\\master");
            File nameFile = new File(rootDirPath + "\\.magit\\name");
            Files.write(Paths.get(nameFile.getPath()), name.getBytes());
            FileUtils.touch(masterFile);
            Files.write(Paths.get(headFile.getPath()), headContent.getBytes());

            System.out.println("Create " + rootDirPath + " success. ");
        } else {
            throw new RepoXmlNotValidException("Root directory location does not exist");
        }
    }

    public static void compareMaps(Map<String, List<FolderItem>> WCmap,
                                   Map<String, List<FolderItem>> LastCommitMap,
                                   String currentWCKey,
                                   String currentCommitKey,
                                   String path,
                                   CalcDiff diff) {
        FolderItemEquator itemsEquator = new FolderItemEquator();
        if (currentCommitKey.equals(currentWCKey))
            return;
        else {
            List<FolderItem> currentCommitFolder = LastCommitMap.get(currentCommitKey);
            List<FolderItem> currentWCFolder = WCmap.get(currentWCKey);

            //deleted files= commitmap-wcmap
            if (!LastCommitMap.isEmpty()) {
                List<FolderItem> deleted = (List<FolderItem>) CollectionUtils.removeAll(currentCommitFolder, currentWCFolder, itemsEquator);
                deleted.stream().
                        forEach(o -> mapLeavesOfPathTree(LastCommitMap, o, path, diff.deleted));
            }
            //added files = wcmap-commitmap
            if (!WCmap.isEmpty()) {
                List<FolderItem> added = (List<FolderItem>) CollectionUtils.removeAll(WCmap.get(currentWCKey), LastCommitMap.get(currentCommitKey), itemsEquator);
                added.stream().
                        forEach(o -> mapLeavesOfPathTree(WCmap, o, path, diff.added));
            }
            //we remain with the common files. go through them and compare
            if (!LastCommitMap.isEmpty()) {
                List<FolderItem> changed = (List<FolderItem>) CollectionUtils.retainAll(WCmap.get(currentWCKey), LastCommitMap.get(currentCommitKey), itemsEquator);
                for (FolderItem item : changed) {
                    Optional<FolderItem> alteredCopy = LastCommitMap.get(currentCommitKey).stream().filter(i -> i.getItemName().equals(item.getItemName()) && i.getType().equals(item.getType())).findFirst();
                    if (item.getType().equals("folder")) {
                        compareMaps(WCmap,
                                LastCommitMap,
                                item.getSha1(),
                                alteredCopy.get().getSha1(),
                                path + "\\" + item.getItemName(),
                                diff);

                    } else if (!alteredCopy.get().getSha1().equals(item.getSha1()))
                        diff.changed.put(path + "\\" + item.getItemName(), alteredCopy.get().getSha1());
                }
            }
        }

    }


    //COMMIT RELATED FUNCTIONS

    public static void mapLeavesOfPathTree(Map<String, List<FolderItem>> mapOfPath, FolderItem item, String path, Map<String, String> leaves) {
        if (!item.getType().equals("folder"))
            leaves.put(path + "\\" + item.getItemName(), item.getSha1());

        else {
            mapOfPath.get(item.getSha1()).stream().forEach(i -> mapLeavesOfPathTree(mapOfPath, i, path + "\\" + item.getItemName(), leaves));
        }

    }

    public static boolean validateRepo(MagitRepository repoToParse) throws RepoXmlNotValidException, FolderIsNotEmptyException, RepositoryAlreadyExistException, IOException {
        return EngineUtils.isRepoLocationValid(repoToParse.getLocation());

    }

    public static boolean checkForChanges(Map<String, List<FolderItem>> mapOfdif, CommitObj commit, String currentRepo) throws IOException {
        Map<String, List<FolderItem>> mapOfLatestCommit = new HashMap<>();
        Map<String, List<FolderItem>> mapOfWC = new HashMap<>();
        String latestCommitRoot;
        String latestCommitSha1 = EngineUtils.readFileToString(currentRepo + BRANCHES + EngineUtils.readFileToString(currentRepo + HEAD_RELATIVE_PATH));
        latestCommitRoot = EngineUtils.getLastCommitRoot(currentRepo);
        String WCSha1 = scanWorkingCopy(currentRepo, mapOfWC);
        if (!WCSha1.equals(latestCommitRoot)) {
            commit.setCommitSHA1(WCSha1);
            commit.setPreviousCommitsSha1(new String[]{latestCommitSha1, ""});
            if (!latestCommitSha1.equals(""))
                mapOfLatestCommit = createLatestCommitMap(latestCommitRoot, currentRepo);
            compareMaps(mapOfWC, mapOfLatestCommit, WCSha1, latestCommitRoot, currentRepo, commit);

            for (String key : mapOfWC.keySet()) {
                if (!mapOfLatestCommit.containsKey(key)) {
                    mapOfdif.put(key, mapOfWC.get(key));
                }
            }
            return true;
        } else
            return false;

    }

    public static Map<String, List<FolderItem>> createLatestCommitMap(String i_rootDirSha, String currentRepo) throws IOException {
        Map<String, List<FolderItem>> result = new HashMap<String, List<FolderItem>>();
        createCommitMapRec(i_rootDirSha, result, currentRepo);
        return result;
    }

    public static void createCommitMapRec(String i_rootDirSha, Map<String, List<FolderItem>> i_commitMap, String currentRepo) throws IOException {
        List<FolderItem> rootDir = EngineUtils.parseToFolderList(currentRepo + OBJECTS + i_rootDirSha + ".zip");
        i_commitMap.put(i_rootDirSha, rootDir);
        for (FolderItem item : rootDir) {
            if (item.getType().equals("folder")) {
                createCommitMapRec(item.getSha1(), i_commitMap, currentRepo);
            } else
                item.setFileContent(String.join("\n", EngineUtils.getZippedFileLines(currentRepo + OBJECTS + item.getSha1() + ".zip")));
        }
    }

    public static Map<String, String> listAllBranches(String currentRepo) throws IOException, RepoXmlNotValidException {
        File branches = FileUtils.getFile(currentRepo + "\\.magit\\branches");
        String sha1;
        Map<String, String> branchesList = new HashMap<>();

        for (File i : branches.listFiles()) {
            if (!(i.getPath().equals(currentRepo + HEAD_RELATIVE_PATH))) {
                if (!i.isDirectory()) {
                    sha1 = EngineUtils.readFileToString(i.getPath());
                    if (sha1.isEmpty() && !i.getName().equals("master")) {
                        throw new RepoXmlNotValidException(String.format("invalid branch file, %s branch file is empty", i.getName()));
                    }
                    branchesList.put(i.getName(), sha1);
                } else Arrays.stream(i.listFiles()).forEach(s -> branchesList.put(i.getName() + "/" + s.getName(),
                        EngineUtils.readFileToString(s.getPath())));
            }

        }
        return branchesList;
    }

    public static void parseMapToWC(Map<String, List<FolderItem>> dirMap, String dirRootSHA1, String sourcePath, String destPath) {
        String newDestPath;

        List<FolderItem> items = dirMap.get(dirRootSHA1);
        for (FolderItem i : items) {
            if (i.getType().equals("folder")) {
                File folder = new File(destPath + "\\" + i.getItemName());
                folder.mkdir();
                parseMapToWC(dirMap, i.getSha1(), sourcePath, destPath + "\\" + i.getItemName());
            } else {
                EngineUtils.extractFile(sourcePath + i.getSha1() + ".zip", i.getSha1(), destPath + "\\" + i.getItemName());

            }
        }


    }

    public static Node createTreeItem(String fileName, String fileContent, Consumer<String> onClick) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        URL mainFXML = MainEngine.class.getResource("/components/TreeItem/TreeItemNodeComponent.fxml");
        loader.setLocation(mainFXML);
        Node singleNode = loader.load();
        TreeItemNode treeItemNode = loader.getController();
        treeItemNode.setName(fileName);
        treeItemNode.setContent(fileContent);
        treeItemNode.setDisplayContent(onClick);
        return singleNode;
    }

    public static TreeItem createDirectoryTreeView(String currRepo, Map<String, List<FolderItem>> result, String rootSha1, Consumer<String> onClick) throws IOException {
        TreeItem root = new TreeItem(currRepo);
        stringifyRepo(result, root, rootSha1, onClick);
        return root;

    }

    public static void stringifyRepo(Map<String, List<FolderItem>> map, TreeItem repo, String root, Consumer<String> onClick) throws IOException {
        List<FolderItem> lst = map.get(root);
        TreeItem leaf;
        for (FolderItem i : lst) {

            if (i.getType().equals("folder")) {
                leaf = addTreeItem(repo, i.getItemName());
                stringifyRepo(map, leaf, i.getSha1(), onClick);
            } else {
                leaf = addTreeItem(repo, "");
                leaf.setGraphic(createTreeItem(i.getItemName(), i.getContent(), onClick));

            }
        }
    }

    public static TreeItem addTreeItem(TreeItem root, String content) {
        TreeItem child = new TreeItem(content);

        root.getChildren().add(child);
        return child;
    }

    public void populateTree(Graph graph) throws IOException, RepoXmlNotValidException {
        EngineUtils.createGraph(graph, m_mainWindowController::displayCommitDetails, m_mainWindowController.getCurrentPath(), this::createCommitTreeView,content-> m_mainWindowController.addCommitTreeView(content));

    }


    public void promptCommit(String currRepo, Consumer<SimpleBooleanProperty> promptUser, Consumer<CommitObj> finalizeCommit, Optional<Runnable> onFinish, Optional<String> mergedCommit) throws IOException {
        CommitObj commit = new CommitObj();
        SimpleBooleanProperty userReply = new SimpleBooleanProperty(false);
        if (checkForChanges(commit.mapOfdif, commit, currRepo)) {
            mergedCommit.ifPresent(commit::setPrevSha1);
            promptUser.accept(userReply);
            if (userReply.getValue() == true) {
                finalizeCommit.accept(commit);
            }
        }
        onFinish.ifPresent(Runnable::run);

    }

    public void viewOpenChanges(String currentRepo, Consumer<CommitObj> onFinish, Consumer<TreeItem> updateWC, UIadapter uIadapter) throws IOException {

        currentRunningTask = new WorkspaceUpdateTask(onFinish, currentRepo, updateWC, uIadapter);
        new Thread(currentRunningTask).start();
    }

    public void finalizeCommit(CommitObj obj, Map<String, List<FolderItem>> mapOfdif, String currentRepo) throws IOException {
        String targetPath = currentRepo + OBJECTS;
        obj.changed.forEach((key, string) -> EngineUtils.ZipFile(DigestUtils.sha1Hex(EngineUtils.readFileToString(key)), key, targetPath));
        obj.added.forEach((key, string) -> EngineUtils.ZipFile(string, key, targetPath));
        foldersToFile(mapOfdif, targetPath);

        String newCommitContent = obj.toString();
        String newCommitSha1 = DigestUtils.sha1Hex(newCommitContent);

        EngineUtils.StringToZipFile(newCommitContent, targetPath, newCommitSha1);
        String currentHead = EngineUtils.readFileToString(currentRepo + HEAD_RELATIVE_PATH);
        EngineUtils.overWriteFileContent(currentRepo + BRANCHES + currentHead, newCommitSha1);
    }

    public void foldersToFile(Map<String, List<FolderItem>> mapOfdif, String targetPath) {

        mapOfdif.forEach((key, item) -> {
            try {

                EngineUtils.StringToZipFile(EngineUtils.listToString(item, "\n"), targetPath, key);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    public List<String> displayLatestCommitHistory(String currRepo) throws IOException {
        String curCommit = EngineUtils.readFileToString(currRepo + BRANCHES + EngineUtils.readFileToString(currRepo + HEAD_RELATIVE_PATH));
        List<String> res = new LinkedList<>();
        List<String> commitContent;
        while (!curCommit.equals("")) {
            commitContent = EngineUtils.getZippedFileLines(currRepo + OBJECTS + curCommit + ".zip");
            res.add("Commit SHA1:" + curCommit + "%s " + EngineUtils.listToString(commitContent.subList(2, 5), " %s "));
            curCommit = commitContent.get(1);
        }
        return res;

    }

    public void switchHeadBranch(String branchName, String currentRepository) {

        String branchFile = currentRepository + BRANCHES + branchName;

        try {
            String skip = currentRepository + "\\.magit";
            String Commitsha1 = EngineUtils.readFileToString(branchFile), rootsha1;
            for (File i : FileUtils.getFile(currentRepository).listFiles()) {
                if (!i.getPath().contains(skip))
                    FileUtils.deleteQuietly(i);
            }

            EngineUtils.overWriteFileContent(currentRepository + HEAD_RELATIVE_PATH, branchName);
            rootsha1 = EngineUtils.getLastCommitRoot(currentRepository);
            Map<String, List<FolderItem>> mapOfCommit = createLatestCommitMap(rootsha1, currentRepository);
            parseMapToWC(mapOfCommit, rootsha1, currentRepository + OBJECTS, currentRepository);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    public TreeItem createCommitTreeView(String sha1){
        try {
            Map<String, List<FolderItem>> mapOfCommit;
            mapOfCommit= createLatestCommitMap(sha1, m_mainWindowController.getCurrentPath());
           return createDirectoryTreeView(m_mainWindowController.getCurrentPath(),mapOfCommit,sha1,i->m_mainWindowController.displayCommitContent(i));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public void checkOut(String branchName, String currRepoPath, UIadapter uiAdapter, Runnable onFinish) throws IOException {
        switchHeadBranch(branchName, currRepoPath);// parse new head to WC
        Map<String, List<FolderItem>> mapOfWC = new HashMap<>();
        String rootSha1 = scanWorkingCopy(currRepoPath, mapOfWC);//create a mp of the new WC
        Platform.runLater(() ->
                {
                    try {


                        uiAdapter.newTreeView(createDirectoryTreeView(currRepoPath, mapOfWC, rootSha1, i -> uiAdapter.displayCommitContent(i)));//display WC
                        m_mainWindowController.setWCmap(mapOfWC);
                        onFinish.run();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

        );

    }

    public void fetch() {
        try {

            updateBranches();
            updateObjects();
           // String rootsha1 = EngineUtils.getLastCommitRoot(m_mainWindowController.getCurrentPath());
           // Map<String, List<FolderItem>> mapOfCommit = createLatestCommitMap(rootsha1, m_mainWindowController.getCurrentPath());
           // parseMapToWC(mapOfCommit, rootsha1, m_mainWindowController.getCurrentPath() + OBJECTS, m_mainWindowController.getCurrentPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        m_mainWindowController.displayMessage("Fetch complete!");

    }

    public void updateObjects() {
        String remotePath = m_mainWindowController.remoteRepoPath + OBJECTS;
        String repoPath = m_mainWindowController.getCurrentPath() + OBJECTS;
        File remoteFiles = new File(remotePath);
        Arrays.stream(remoteFiles.listFiles()).forEach(i ->
                { if(!FileUtils.getFile(repoPath + i.getName()).exists())
                copy(Paths.get(i.getPath()), Paths.get(repoPath + i.getName()));});

    }

    public void pull() throws IOException {
        String remoteBranches=m_mainWindowController.remoteRepoPath+BRANCHES;
        String localBranches=m_mainWindowController.getCurrentPath()+BRANCHES;
        String RBs=localBranches+"\\" + m_mainWindowController.remoteRepoName;
        String RTB= EngineUtils.readFileToString(localBranches+"\\"+m_mainWindowController.headBranchName.getValue());
        String RB= EngineUtils.readFileToString(RBs+"\\"+m_mainWindowController.headBranchName.getValue());
        String remoteBranch = EngineUtils.readFileToString(remoteBranches + m_mainWindowController.headBranchName.getValue());
        if(!RB.equals(RTB)){
            Platform.runLater(()-> m_mainWindowController.displayMessage("The remote branch is not synched with the remote tracking branch." +
                    "Please push your updates"));
            return;
        }
        else {
            try {
                importCommitHistory(m_mainWindowController.remoteRepoPath, m_mainWindowController.getCurrentPath(), remoteBranch, RB);
            } catch (IOException e) {
                e.printStackTrace();
            }
            EngineUtils.overWriteFileContent(localBranches + "\\" + m_mainWindowController.headBranchName.getValue(),
                    remoteBranch);
            EngineUtils.overWriteFileContent(RBs + "\\" + m_mainWindowController.headBranchName.getValue(),
                    remoteBranch);
            String rootSha1=EngineUtils.getZippedFileLines(m_mainWindowController.getCurrentPath()+OBJECTS+ remoteBranch+".zip").get(0);
            parseMapToWC(createLatestCommitMap(rootSha1,
                    m_mainWindowController.getCurrentPath()),rootSha1,m_mainWindowController.getCurrentPath()+OBJECTS, m_mainWindowController.getCurrentPath());

        }

    }
    public void importCommit(Map<String, List<FolderItem>> commit, String SourcePath, String DestPath){

        commit.forEach((sha1,list)->list.forEach(i-> {
            if (!FileUtils.getFile(DestPath + i.getSha1() + ".zip").exists())
                copy(Paths.get(SourcePath + i.getSha1() + ".zip"), Paths.get(DestPath + i.getSha1() + ".zip"));
        }));


    }
    public void importCommitHistory(String SourceRepoPath,String DestRepoPath, String from, String to) throws IOException {
        String current= from;
        Map<String, List<FolderItem>> currMap;
        while(!current.equals(to)){
            copy(Paths.get(SourceRepoPath+OBJECTS+from+".zip"),Paths.get(DestRepoPath+OBJECTS+from+".zip"));//copy the commit file
            List <String> commitContents= EngineUtils.getZippedFileLines(SourceRepoPath+OBJECTS+ from+ ".zip");
            currMap=createLatestCommitMap(commitContents.get(0),SourceRepoPath);
            importCommit(currMap,SourceRepoPath+OBJECTS,
                    DestRepoPath+OBJECTS);
            copy(Paths.get(SourceRepoPath+OBJECTS + commitContents.get(0)+ ".zip"), Paths.get(DestRepoPath+OBJECTS +commitContents.get(0)+ ".zip"));
            if(current.equals(from)) parseMapToWC(currMap, commitContents.get(0),DestRepoPath+OBJECTS,DestRepoPath);

            current= commitContents.get(1).split(",")[0];
        }

    }


    public void push() throws IOException {

        if(checkForChanges(new HashMap<>(), new CommitObj(), m_mainWindowController.remoteRepoPath))
        {
            Platform.runLater(()->{m_mainWindowController.displayMessage("Open changes have been detected in the RR. Cannot perform push operation");}
            );
            return;
        }
        String remoteBranches=m_mainWindowController.remoteRepoPath+BRANCHES;
        String localBranches=m_mainWindowController.getCurrentPath()+BRANCHES;
        String RBs=localBranches + m_mainWindowController.remoteRepoName;
        String RTB= EngineUtils.readFileToString(localBranches+"\\"+m_mainWindowController.headBranchName.getValue());     //check RR for open changes
        String RB= EngineUtils.readFileToString(RBs+"\\"+m_mainWindowController.headBranchName.getValue());
        String remoteBranch = EngineUtils.readFileToString(remoteBranches + m_mainWindowController.headBranchName.getValue());
        if(!RB.equals(remoteBranch)){
            Platform.runLater(()-> m_mainWindowController.displayMessage("Please fetch updates prior to performing a push action"));
            return;
        }
        importCommitHistory(m_mainWindowController.getCurrentPath(),m_mainWindowController.remoteRepoPath,RTB,RB);
        EngineUtils.overWriteFileContent(remoteBranches + "\\" + m_mainWindowController.headBranchName.getValue(),
                RTB);
        EngineUtils.overWriteFileContent(RBs + "\\" + m_mainWindowController.headBranchName.getValue(),
                                                       RTB);

                //if none,get head. make sure RB= branch in RR.

        //if equal create a map of all the new commits made in LR on the RTB since RB
        //copy all new commits into the objects folder in RR and point branch in RR to RB
        //parse latest commit in RR WC
        //make RB point to RTB

        //make a map of all the commits
    }



    public void updateBranches() throws IOException {
        String remotePath = m_mainWindowController.remoteRepoPath;
        String repoPath = m_mainWindowController.getCurrentPath();
        File remoteBranches = new File(remotePath + BRANCHES);
        Arrays.stream(remoteBranches.listFiles()).forEach(i ->{if(!i.getName().equals("HEAD")) copy(Paths.get(i.getPath()), Paths.get(repoPath+BRANCHES+"\\"+m_mainWindowController.remoteRepoName+"\\"+ i.getName()));});

        //        File localBranches = new File(repoPath + BRANCHES);
        //  if(EngineUtils.readFileToString(remotePath+HEAD_RELATIVE_PATH).equals(repoPath+ HEAD_RELATIVE_PATH)) throw new RepoXmlNotValidException("Head in the remote repository is different. ");
//        List<File> remotes = Arrays.stream(remoteBranches.listFiles()).collect(Collectors.toList());
//        List<File> locals = Arrays.stream(localBranches.listFiles()).collect(Collectors.toList());
//        FileEquator equator = new FileEquator();
        //Collection<File> commonBranches;
       // copyFolder(Paths.get(remotePath+ BRANCHES), Paths.get(repoPath+BRANCHES+"\\"+m_mainWindowController.remoteRepoName));
//        List<File> commonBranches = (List<File>) CollectionUtils.retainAll(remotes, locals, equator);
//        List<File> newBranches = (List<File>) CollectionUtils.removeAll(remotes, locals, equator);
//        File RTBfile = new File(localBranches.getPath() + m_mainWindowController.remoteRepoName);
//        if (!RTBfile.exists()) FileUtils.forceMkdir(RTBfile);

}

    public List setCommitDiff(String i_currentRepositoryPath, CommitObj i_commitObj) throws IOException {
        List<List<Map<String, String>>> result = new ArrayList<>();
        List<Map<String, String>> diffFirstPrevCommit = new ArrayList<>();

        List<String> latestCommit = EngineUtils.getZippedFileLines(i_currentRepositoryPath + OBJECTS + i_commitObj.getFirstPrecedingSha1() + ".zip");
        String previousCommitRoot = latestCommit.get(EngineUtils.CommitMembers.ROOT_DIR_SHA1.ordinal());

        Map<String, List<FolderItem>> mapOfPreviousCommit = createLatestCommitMap(previousCommitRoot, i_currentRepositoryPath);
        Map<String, List<FolderItem>> mapOfCurrCommit = createLatestCommitMap(i_commitObj.getRootDirSha1(), i_currentRepositoryPath);

        MainEngine.compareMaps(mapOfCurrCommit, mapOfPreviousCommit, i_commitObj.getRootDirSha1(), previousCommitRoot, i_currentRepositoryPath, i_commitObj);
        diffFirstPrevCommit.add(EngineUtils.Diff.DELETED.ordinal(), i_commitObj.deleted);
        diffFirstPrevCommit.add(EngineUtils.Diff.ADDED.ordinal(), i_commitObj.added);
        diffFirstPrevCommit.add(EngineUtils.Diff.CHANGED.ordinal(), i_commitObj.changed);

        result.add(diffFirstPrevCommit);
        if (!i_commitObj.getSecondPrecedingSha1().isEmpty()) {
            latestCommit = EngineUtils.getZippedFileLines(i_currentRepositoryPath + OBJECTS + i_commitObj.getSecondPrecedingSha1() + ".zip");
            previousCommitRoot = latestCommit.get(EngineUtils.CommitMembers.ROOT_DIR_SHA1.ordinal());

            mapOfPreviousCommit = createLatestCommitMap(previousCommitRoot, i_currentRepositoryPath);
            mapOfCurrCommit = createLatestCommitMap(i_commitObj.getRootDirSha1(), i_currentRepositoryPath);

            MainEngine.compareMaps(mapOfCurrCommit, mapOfPreviousCommit, i_commitObj.getRootDirSha1(), previousCommitRoot, i_currentRepositoryPath, i_commitObj);
            diffFirstPrevCommit.add(i_commitObj.deleted);
            diffFirstPrevCommit.add(i_commitObj.added);
            diffFirstPrevCommit.add(i_commitObj.changed);

            result.add(diffFirstPrevCommit);

        }
        return result;
    }

    public void commit(String currRepo, Consumer<CommitObj> commitConsumer, Optional<Runnable> onFinish) {
        CommitObj obj = new CommitObj();

        try {
            if (checkForChanges(obj.mapOfdif, obj, currRepo)) {
                commitConsumer.accept(obj);
            } else
                onFinish.ifPresent(Runnable::run);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void loadRepo(UIadapter uiadapter, String absolutePath) {
        if (!isAMagitRepo(absolutePath)) {
            Platform.runLater(() ->
                    m_mainWindowController.displayMessage("Specified directory is not a valid repository")

            );
        } else {
            RepoLoader loader = new RepoLoader(absolutePath);
            currentRunningTask = new LoadRepoTask(absolutePath, uiadapter, loader);
            new Thread(currentRunningTask).start();
        }
    }

    public void loadRepoFromXML(UIadapter uiadapter, String xmlPath) {
        XMLloader loader = new XMLloader(xmlPath);
        currentRunningTask = new LoadRepoTask(xmlPath, uiadapter, loader);
        new Thread(currentRunningTask).start();

    }

    public void mergeAction(String branchToMerge, String currentHead, UIadapter uIadapter) throws IOException {
        String repoPath = m_mainWindowController.getCurrentPath();
        currentRunningTask = new MergeTask(currentHead, branchToMerge, repoPath, this, uIadapter);
        new Thread(currentRunningTask).start();


    }

    public void initNewRepo(String rooDirPath, String name, UIadapter uiadapter) {
        currentRunningTask = new InitRepoTask(rooDirPath, name, uiadapter);
        new Thread(currentRunningTask).start();

    }

    public void createNewBranch(String name, Consumer<String> alert, String repoPath) {
        String relativePath = repoPath + BRANCHES;
        Platform.runLater(() -> {
            if (FileUtils.getFile(relativePath + name).exists()) {
                alert.accept("A branch by this name already exists");
                return;
            } else {
                try {
                    EngineUtils.createBranchFile(repoPath, name);
                    alert.accept("Branch created successfuly");
                } catch (IOException e) {
                    alert.accept("An error has occured. Please restart the program");
                }
            }
        });
    }

    public boolean isAMagitRepo(String path) {

        if (FileUtils.getFile(path + "\\.magit").exists())
            return true;
        return false;
    }

    public void cloneRepo(String from, String destination, String repoName) {
        String to = destination + repoName;
        SimpleBooleanProperty userReply = new SimpleBooleanProperty(true);
        File dest = FileUtils.getFile(to);
        if (dest.exists()) {
            m_mainWindowController.displayPrompt(userReply, "Directory already exists. Would you like to override it and proceed? \n" +
                    " (this will permanently delete all its contents.) ");
            if (userReply.getValue() == false) return;
            File[] dirItems = dest.listFiles();
            if (dirItems.length > 0) {
                for (File file : dirItems) {
                    FileUtils.deleteQuietly(file);
                }
            }
        }
        try {
            FileUtils.forceMkdir(dest);
            copyFolder(Paths.get(from), Paths.get(to));
            String headBranch= EngineUtils.readFileToString(to+BRANCHES+"HEAD");
            File prefix = new File(to + BRANCHES + Paths.get(from).getFileName());
            FileUtils.forceMkdir(prefix);
            Arrays.stream(FileUtils.getFile(to + BRANCHES).listFiles()).filter(i -> !i.isDirectory() && !i.getName().equals("HEAD"))
                    .forEach(i -> copy(Paths.get(i.getPath()), Paths.get(prefix.getPath() + "\\" + i.getName())));

            Arrays.stream(FileUtils.getFile(to+BRANCHES).listFiles()).forEach(i-> {if(!i.getName().equals("HEAD")&& !i.getName().equals(headBranch)&& !i.isDirectory())i.delete();});//delete all RTB
            EngineUtils.stringToTextFile(to+"\\.magit\\", from+"\n"+ FileUtils.getFile(from).getName(),"remote");

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void copyFolder(Path src, Path dest) throws IOException {
        Files.walk(src)
                .forEach(source -> copy(source, dest.resolve(src.relativize(source))));
    }

    private void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, REPLACE_EXISTING);
        } catch (AccessDeniedException e) {
            Platform.runLater(()->
            {m_mainWindowController.displayMessage(String.format("It appears that %s is being used by a different process. Please close the file and try again",e.getFile()));}
         );
        }
        catch (IOException e) {
            Platform.runLater(()->
                    m_mainWindowController.displayMessage("Something went wrong")
            );
        }
    }

    public String createRTB(String i) {
           if(FileUtils.getFile(m_mainWindowController.getCurrentPath()+ BRANCHES + i).exists()) {
          // m_mainWindowController.displayMessage("RTB for this remote branch already exists");
            return "";}
        else {
            String content= EngineUtils.readFileToString(m_mainWindowController.getCurrentPath()+BRANCHES+"\\"+ m_mainWindowController.remoteRepoName+"\\"+i);
            EngineUtils.stringToTextFile(m_mainWindowController.getCurrentPath()+ BRANCHES ,
                content,i);
        return content;}

    }

    public void resetBranch(String i) throws IOException {
        String sha1= m_mainWindowController.getCurrentPath()+OBJECTS+i+".zip";
        if(FileUtils.getFile(sha1).exists()) {
            if(EngineUtils.getZippedFileLines(sha1).get(5).equals("commit"))
            {EngineUtils.overWriteFileContent(m_mainWindowController.getCurrentPath() + BRANCHES + m_mainWindowController.headBranchName.getValue(), i);
            return;}
            else {
                m_mainWindowController.displayMessage("The SHA1 is not a commit SHA1");
            }
            m_mainWindowController.displayMessage("The SHA1 is not a commit SHA1");
        }

    }

    public static class FileEquator implements Equator<File> {
        @Override
        public boolean equate(File e, File t1) {
            return e.getName().equals(t1.getName());
        }

        @Override
        public int hash(File e) {
            return 0;
        }
    }

    public static class FolderItemEquator implements Equator<FolderItem> {
        @Override
        public boolean equate(FolderItem t1, FolderItem t2) {
            return (t1.getItemName().equals(t2.getItemName()) && t1.getType().equals(t2.getType()));
        }

        @Override
        public int hash(FolderItem folderItem) {
            return (folderItem.getItemName() + folderItem.getType()).hashCode();
        }

    }


}
