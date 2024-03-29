package moduleClasses;

import Exceptions.FolderIsNotEmptyException;
import Exceptions.RepoXmlNotValidException;
import Exceptions.RepositoryAlreadyExistException;
import com.fxgraph.edges.Edge;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.Model;
import components.commitTree.CommitTreeLayout;
import components.commitTree.commitNode.CommitNode;
import javafx.scene.control.TreeItem;
import jaxbClasses.*;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static common.MagitResourcesConstants.*;

public class EngineUtils {
    private static String m_relativeBranchPath = "\\.magit\\branches";
    private static String m_relativeObjectsPath = "\\.magit\\objects";

    public static List<FolderItem> parseToFolderList(String i_filePath) throws IOException {

        List<FolderItem> result = new ArrayList<>();
        String[] itemDetails;
        List<String> folderItems = getZippedFileLines(i_filePath);
        for (String line : folderItems) {
            itemDetails = line.split(",");
            result.add(new FolderItem(itemDetails));
        }
        return result;
    }

    public static List<String> getZippedFileLines(String i_filePath) throws IOException {
        BufferedReader reader;
        String currentLine;
        InputStream stream;
        List<String> fileLines = new ArrayList<>();

        ZipFile zipFile = new ZipFile(i_filePath);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        ZipEntry entry = entries.nextElement();
        stream = zipFile.getInputStream(entry);
        reader = new BufferedReader(new InputStreamReader(stream));

        while ((currentLine = reader.readLine()) != null) {
            fileLines.add(currentLine);
        }

        return fileLines;
    }


    public static String getLastCommitRoot(String i_currentRepository) throws IOException {
        String branchName;
        List<String> res;
        String headFile = i_currentRepository + m_relativeBranchPath + "\\HEAD";
        branchName = readFileToString(headFile);
        String branchFile = i_currentRepository + m_relativeBranchPath + "\\" + branchName;
        String lastCommitSha1 = readFileToString(branchFile);


        if (lastCommitSha1.equals(""))
            return "";
        res = getZippedFileLines(i_currentRepository + OBJECTS + lastCommitSha1 + ".zip");
        String result = res.get(0);
        return result;
    }


    public static void StringToZipFile(String content, String targetPath, String fileName) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append(content);
        File f = new File(targetPath + fileName + ".zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
        ZipEntry e = new ZipEntry(fileName);
        out.putNextEntry(e);
        byte[] data = sb.toString().getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
        out.close();
    }

    public static void overWriteFileContent(String path, String Content) {
        try {
            FileWriter fw = new FileWriter(path, false);
            fw.write(Content);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void ZipFile(String zipFileName, String sourceFile, String zipTarget) {

        try {
            FileOutputStream fos = new FileOutputStream(zipTarget + zipFileName + ".zip");
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(sourceFile);
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            zipOut.close();
            fis.close();
            fos.close();

        } catch (FileNotFoundException ex) {
            System.err.format("The file %s does not exist", sourceFile);
        } catch (IOException ex) {
            System.err.println("I/O error: " + ex);
        }

    }

    public static String listToString(List<?> list, String delimiter) {
        List<String> res = new LinkedList<>();
        list.forEach(i -> res.add(i.toString()));
        String temp = String.join(delimiter, res);
        return temp;
    }


    public static void extractFile(String zipPath, String entry, String destPath) {
        try {
            File f = new File(destPath);
            String content = String.join("\n", getZippedFileLines(zipPath));
            Files.write(Paths.get(destPath), content.getBytes());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createBranchFile(String destPath, String branchName) throws IOException {

        File newBranch = new File(destPath + BRANCHES + branchName);
        String head = readFileToString(destPath + HEAD_RELATIVE_PATH);
        String mainBranch = readFileToString(destPath + BRANCHES + head);
        Files.write(Paths.get(newBranch.getPath()), mainBranch.getBytes());

    }

    public static void stringToTextFile(String destPath, String content, String fileName) {
        File newFile = new File(destPath + "\\" + fileName);
        try {
            FileWriter fw = new FileWriter(newFile.getPath(), true);
            fw.write(content);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String readFileToString(String filePath) {
        String content = "";

        try {
            content = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }

//    public static boolean isACommit(String sha1, String rootPath) throws IndexOutOfBoundsException, IOException {
//            return (getZippedFileLines(rootPath + OBJECTS + sha1 + ".zip").get(5).equalsIgnoreCase("commit"));
//
//
//    }


    public static boolean isRepoLocationValid(String i_repoLocation) throws RepositoryAlreadyExistException, FolderIsNotEmptyException, RepoXmlNotValidException {
        if (FileUtils.getFile(i_repoLocation).exists()) {

            if (FileUtils.getFile(i_repoLocation).isFile()) {
                throw new RepoXmlNotValidException("location given is a file location, not directory");
            }
            if (FileUtils.getFile(i_repoLocation).listFiles().length > 0) {
                if (FileUtils.getFile(i_repoLocation + "\\.magit").exists())//true-> folder has files/sub directories
                {
                    throw new RepositoryAlreadyExistException(i_repoLocation);
                } else {
                    throw new FolderIsNotEmptyException(i_repoLocation);
                }
            }

        }
        return true;
    }


    public static Map<String, MagitBlob> createRepoBlobsMap(MagitBlobs i_magitBlobs) throws RepoXmlNotValidException {
        Map<String, MagitBlob> result = new HashMap<>();
        String currBlobID;
        for (MagitBlob blob : i_magitBlobs.getMagitBlob()) {
            currBlobID = blob.getId();
            if (result.containsKey(currBlobID)) {
                throw new RepoXmlNotValidException(String.format("Xml has two or more blobs with the same id \nid: {}", currBlobID));
            } else {
                result.put(currBlobID, blob);
            }
        }
        return result;

    }

    public static Map<String, MagitSingleFolder> createRepoFoldersMaps(MagitFolders i_magitFolders, Map<String, MagitBlob> i_blobsMap) throws RepoXmlNotValidException {
        String currFolderID;
        Map<String, MagitSingleFolder> result = new HashMap<>();
        for (MagitSingleFolder folder : i_magitFolders.getMagitSingleFolder()) {
            currFolderID = folder.getId();

            if (result.containsKey(currFolderID)) {
                throw new RepoXmlNotValidException(String.format("XML has two or more folders with the same id \nerror occurred in folder: %s", currFolderID));
            }
            for (Item item : folder.getItems().getItem()) {
                if (item.getType().equals("blob")) {
                    if (!i_blobsMap.containsKey(item.getId())) {
                        throw new RepoXmlNotValidException((String.format("Illegal XML: Folder contains a blob with an id that does not exist\n\nerror occurred in folder: %s", currFolderID)));
                    }
                } else {
                    if (currFolderID.equals(item.getId())) {
                        throw new RepoXmlNotValidException(
                                String.format("Illegal XML: Folder contains a sub folder with the same id \nerror occurred in folder: %s", currFolderID));
                    }
                }

            }

            result.put(currFolderID, folder);

        }
        return result;
    }

    public static Map<String, CommitObj> createRepoCommitMap(MagitCommits i_magitCommits, Map<String, MagitSingleFolder> i_foldersMap) throws RepoXmlNotValidException {
        Map<String, CommitObj> result = new HashMap<>();
        String currCommitID;
        for (MagitSingleCommit commit : i_magitCommits.getMagitSingleCommit()) {
            currCommitID = commit.getId();
            if (result.containsKey(currCommitID)) {
                throw new RepoXmlNotValidException(String.format("Xml has two or more commits with the same id \nid: %s", currCommitID));
            }
            if (!i_foldersMap.containsKey(commit.getRootFolder().getId())) {
                throw new RepoXmlNotValidException(String.format("Illegal XML: Commit root folder does not exist\nerror occurred in commit: %s", currCommitID));
            }

            if (!i_foldersMap.get(commit.getRootFolder().getId()).isIsRoot()) {
                throw new RepoXmlNotValidException(String.format("Illegal XML: Commit has root folder that is actually not a root folder\nerror occurred in commit: %s", currCommitID));
            }

            ArrayList<String> precedingCommits = new ArrayList(2);
            precedingCommits.add("");
            precedingCommits.add("");
            if (!(commit.getPrecedingCommits() == null || commit.getPrecedingCommits().getPrecedingCommit().size() == 0)) {
                precedingCommits.set(0, commit.getPrecedingCommits().getPrecedingCommit().get(0).getId());
                if (commit.getPrecedingCommits().getPrecedingCommit().size() == 2)
                    precedingCommits.set(1, commit.getPrecedingCommits().getPrecedingCommit().get(1).getId());
            }

            CommitObj currCommit = new CommitObj(commit.getDateOfCreation(), commit.getMessage(), commit.getAuthor(), precedingCommits, commit.getRootFolder().getId());
            result.put(currCommitID, currCommit);

        }
        return result;
    }

    public static Map<String, String> createRepoBranchMap(MagitBranches i_branches, Set<String> i_commitKeys) throws RepoXmlNotValidException {

        Map<String, String> result = new HashMap<>();
        List<MagitSingleBranch> branches = i_branches.getMagitSingleBranch();
        for (MagitSingleBranch branch : branches) {
            if ((!i_commitKeys.contains(branch.getPointedCommit().getId()) && (!branch.getName().equals("master"))) && (!branch.getPointedCommit().equals(""))) {
                throw new RepoXmlNotValidException(String.format("Illegal XML: branch is pointing to commit that does not exist\nerror occurred in branch: %s", branch.getName()));
            }
            for (MagitSingleBranch otherBranch : branches) {
                if (!otherBranch.equals(branch) && otherBranch.getName().equals(branch.getName())) {
                    throw new RepoXmlNotValidException(String.format("Illegal XML: Commit points to a non-existant root folder \nerror occurred in commit: %s", branch.getName()));
                }
            }
            result.put(branch.getName(), branch.getPointedCommit().getId());
        }
        if (result.get(i_branches.getHead()) == null)
            throw new RepoXmlNotValidException("Illegal XML: Head is not an actual branch");
        return result;

    }

    public static boolean isHEADValid(MagitRepository i_magitRepository, Set<String> i_branchesNames) {
        return i_branchesNames.contains(i_magitRepository.getMagitBranches().getHead());
    }

    public static void createGraph(Graph i_graph, Consumer<CommitObj> i_onClick, String  i_pathToRepoFolder, Function<String, TreeItem> createTreeView, Consumer <TreeItem> display) throws IOException, RepoXmlNotValidException {
        Map<String, CommitObj> commits = getAllApproachableCommits(i_pathToRepoFolder);
        addBranchesToCommits(commits, i_pathToRepoFolder);
        Model treeModel = i_graph.getModel();
        i_graph.beginUpdate();

        List<CommitNode> cells = commits.values().stream().map(commitObj -> new CommitNode(commitObj, i_onClick, createTreeView.apply(commitObj.rootDirSha1),display)).collect(Collectors.toCollection(ArrayList::new));
        for (CommitNode commitNode : cells) {
            treeModel.addCell(commitNode);
        }

        CommitNode rootCell = cells.stream().filter(
                cell ->
                        cell.commitDetails.PreviousCommitsSha1.get(0).isEmpty() && cell.commitDetails.PreviousCommitsSha1.get(1).isEmpty())
                .findFirst().get();
        createCommitTree(cells, rootCell, treeModel);


        i_graph.endUpdate();

        i_graph.layout(new CommitTreeLayout());
    }

    private static void addBranchesToCommits(Map<String, CommitObj> commits, String i_pathToRepoFolder) {
        File[] branches = FileUtils.getFile(i_pathToRepoFolder + m_relativeBranchPath).listFiles();
        for (File branch : branches) {
            if (!branch.getName().equals("HEAD")&&!branch.isDirectory()) {
                String branchCurrCommit = EngineUtils.readFileToString(branch.getPath());
                commits.get(branchCurrCommit).addPointingBranch(branch.getName());
            }
        }

    }


    private static void createCommitTree(List<CommitNode> i_Nodes, CommitNode i_rootCell, Model i_tree) {
        List<CommitNode> childrenCommitNodes = i_Nodes.stream().filter(
                commitNode -> commitNode.commitDetails.PreviousCommitsSha1.contains(i_rootCell.commitDetails.getCommitSha1()))
                .collect(Collectors.toCollection(ArrayList::new));
        for (CommitNode childCommitNode : childrenCommitNodes) {
            i_tree.addEdge(new Edge(i_rootCell, childCommitNode));
            createCommitTree(i_Nodes, childCommitNode, i_tree);

        }
    }

    public static Map<String, CommitObj> getAllApproachableCommits(String i_pathToRepoFolder) throws RepoXmlNotValidException, IOException {
        if (FileUtils.getFile(i_pathToRepoFolder).isFile()) {
            throw new RepoXmlNotValidException("location given is a file location, not directory");
        }
        File[] branches = FileUtils.getFile(i_pathToRepoFolder + m_relativeBranchPath).listFiles();
        Map<String, CommitObj> result = new HashMap<>();
        for (File branch : branches) {
            if (!branch.getName().equals("HEAD")&& !branch.isDirectory()&& !branch.getName().contains("/")) {
                String branchCurrCommit = EngineUtils.readFileToString(branch.getPath());
                populateCommitMap(result, i_pathToRepoFolder, branchCurrCommit);
            }
        }
        return result;
    }

    private static void populateCommitMap(Map<String, CommitObj> i_mapToPopulate, String i_pathToRepoFolder, String i_commitSha1) throws IOException {
        if (i_commitSha1.equals("") || i_mapToPopulate.containsKey(i_commitSha1)) {
            return;
        }
        String currCommitPath = i_pathToRepoFolder + m_relativeObjectsPath + "\\" + i_commitSha1 + ".zip";
        List<String> commitDetails = EngineUtils.getZippedFileLines(currCommitPath);
        ArrayList <String> prevCommits= new ArrayList<>(Arrays.asList(commitDetails.get(CommitMembers.PREV_COMMIT.ordinal()).split(",")));
        if (prevCommits==null)prevCommits.add("");
        if(prevCommits.size()==1) prevCommits.add("");
        CommitObj branchCurrCommit = new CommitObj(commitDetails.get(CommitMembers.DATE.ordinal()),
                commitDetails.get(CommitMembers.COMMENT.ordinal()),
                commitDetails.get(CommitMembers.SUBMITTER.ordinal()),
                commitDetails.get(CommitMembers.ROOT_DIR_SHA1.ordinal()),
                new ArrayList<>(Arrays.asList(commitDetails.get(CommitMembers.PREV_COMMIT.ordinal()).split(","))),
                i_commitSha1,
                null
        );
        i_mapToPopulate.put(i_commitSha1, branchCurrCommit);
        populateCommitMap(i_mapToPopulate, i_pathToRepoFolder, branchCurrCommit.getFirstPrecedingSha1());
        populateCommitMap(i_mapToPopulate, i_pathToRepoFolder, branchCurrCommit.getSecondPrecedingSha1());
    }


    public enum CommitMembers {
        ROOT_DIR_SHA1, PREV_COMMIT, SUBMITTER, DATE, COMMENT
    }
    public enum Diff{
        DELETED,ADDED,CHANGED
    }

}


