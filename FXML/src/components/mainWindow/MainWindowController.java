package components.mainWindow;


import Exceptions.RepoXmlNotValidException;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.PannableCanvas;
import components.Merger.MergeController;
import components.branchButton.SingleBranchController;
import components.choiceWindowComponent.PopupController;
import components.messageWindow.MessageWindowController;
import components.userMessageGetterComponent.UserInputGetter;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import moduleClasses.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static common.MagitResourcesConstants.BRANCHES;


public class MainWindowController {

    public SimpleStringProperty headBranchName;
    //  private SimpleBooleanProperty openChangesDetected;
    public MainEngine engine;
    public Map<String, List<FolderItem>> WCmap;
    public Map<String, String> branches;
    public Map<String, String> remoteBranches;
    public String remoteRepoPath;
    public String remoteRepoName;
    @FXML
    Button commitBtn;
    @FXML
    Button pushBtn;
    @FXML
    Button pullBtn;
    @FXML
    Button cloneBtn;
    @FXML
    Button branchBtn;
    @FXML
    Button userBtn;
    @FXML
    MenuButton repositoryMenu;
    @FXML
    MenuItem addRepo;
    @FXML
    MenuItem loadRepo;
    @FXML
    MenuItem initRepo;
    @FXML
    Button fetchBtn;
    @FXML
    Button historyBtn;
    @FXML
    Button statusBtn;
    @FXML
    ScrollPane commitTreeScrollPane;
    @FXML
    Pane statusTab;
    @FXML
    Label repoName;
    @FXML
    Label repoPath;
    @FXML
    Label headBranch;
    @FXML
    VBox branchesMenuPane;
    @FXML
    TreeView FolderTreeView;
    @FXML
    Button openChangesBtn;
    @FXML
    Label deletedItems;
    @FXML
    Label changedItems;
    @FXML
    Label addedItems;
    @FXML
    Pane TreeViewContainer;
    @FXML
    Label fileContentDisplay;
    @FXML
    Pane historyTab;
    @FXML
    Label commitDetails;
    @FXML
    Label deletedItemsHistory;
    @FXML
    Label addedItemsHistory;
    @FXML
    Label changedItemsHistory;
    @FXML
    Label selectedCommitFileContent;
    @FXML
    TreeView commitFolderTree;
    @FXML
    Pane commitContentContainer;
    @FXML
    Label userNameLabel;
    @FXML
    Button resetButton;
    @FXML
    Button rebaseBranchbtn;
    private SimpleBooleanProperty isRepositoryLoaded;
    private SimpleStringProperty currentRepositoryPath;
    private SimpleStringProperty userName;
    private SimpleStringProperty currentRepositoryName;
    private SimpleStringProperty deletedItemsList;
    private SimpleStringProperty changedItemsList;
    private SimpleStringProperty addedItemsList;
    private SimpleStringProperty deletedItemsHistoryList;
    private SimpleStringProperty changedItemsHistoryList;
    private SimpleStringProperty addedItemsHistoryList;
    private SimpleStringProperty currentContentDisplayed;
    private SimpleStringProperty selectedSha1;
    private SimpleBooleanProperty sha1Selected;

    //  private Timer timer;
    private Stage primaryStage;
    private SimpleBooleanProperty isRemoteTracking;



    public MainWindowController() {
        isRepositoryLoaded = new SimpleBooleanProperty(false);
        userName = new SimpleStringProperty("Administrator");
        //    openChangesDetected = new SimpleBooleanProperty(false);
        currentRepositoryPath = new SimpleStringProperty("");
        currentRepositoryName = new SimpleStringProperty("");
        headBranchName = new SimpleStringProperty("");
        changedItemsList = new SimpleStringProperty("");
        deletedItemsList = new SimpleStringProperty("");
        addedItemsList = new SimpleStringProperty("");
        currentContentDisplayed = new SimpleStringProperty("");
        this.remoteRepoPath = new String();
        this.remoteRepoName = new String();
        this.isRemoteTracking = new SimpleBooleanProperty(false);
        deletedItemsHistoryList = new SimpleStringProperty("");
        changedItemsHistoryList = new SimpleStringProperty("");
        addedItemsHistoryList = new SimpleStringProperty("");
        selectedSha1= new SimpleStringProperty("");
        sha1Selected=new SimpleBooleanProperty(false);



    }
    public void createCommitMap(){
        Graph tree = new Graph();

        PannableCanvas canvas = tree.getCanvas();
        commitTreeScrollPane.setContent(canvas);

        try {
            engine.populateTree(tree);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RepoXmlNotValidException e) {
            e.printStackTrace();
        }
        Platform.runLater(() -> {
            tree.getUseViewportGestures().set(false);
            tree.getUseNodeGestures().set(false);
        });

    }

    public String getCurrentPath() {
        return currentRepositoryPath.getValue();
    }

    public void displayCommitDetails(CommitObj i_CommitObj) {
        sha1Selected.set(true);
        selectedSha1.set(i_CommitObj.getCommitSha1());
        String secondPrevCommit;
        boolean isThereSecondPrevCommit = !i_CommitObj.getSecondPrecedingSha1().isEmpty();
        if (isThereSecondPrevCommit) {
            secondPrevCommit = String.format(" , " + i_CommitObj.getSecondPrecedingSha1());
        } else {
            secondPrevCommit = "";
        }
        String commitDetailsText = String.format("SHA1: " + i_CommitObj.getCommitSha1() + "\n" +
                "Comment: " + i_CommitObj.commitMessage + "\n" +
                "Created by: " + i_CommitObj.getUserName() + "\n" +
                "Date: " + i_CommitObj.dateCreated + "\n" +
                "Previous Commits: " + i_CommitObj.getFirstPrecedingSha1() + "%s", secondPrevCommit);
        commitDetails.setText(commitDetailsText);
        try {

            List<List<Map<String, String>>> diff = (List<List<Map<String, String>>>) engine.setCommitDiff(currentRepositoryPath.getValue(), i_CommitObj);
            setChangesLabelByTypeOfChange(diff, i_CommitObj.getFirstPrecedingSha1(), i_CommitObj.getSecondPrecedingSha1(), EngineUtils.Diff.DELETED.ordinal());
            setChangesLabelByTypeOfChange(diff, i_CommitObj.getFirstPrecedingSha1(), i_CommitObj.getSecondPrecedingSha1(), EngineUtils.Diff.ADDED.ordinal());
            setChangesLabelByTypeOfChange(diff, i_CommitObj.getFirstPrecedingSha1(), i_CommitObj.getSecondPrecedingSha1(), EngineUtils.Diff.CHANGED.ordinal());
        } catch (IOException e) {

        }


    }

    private void setChangesLabelByTypeOfChange(List<List<Map<String, String>>> diff, String i_firstPrevCommit, String i_secondPrevCommit, int i_changeTypeIndex) {
        boolean isThereSecondPrevCommit = !i_secondPrevCommit.isEmpty();
        StringBuilder labelText;
        if (diff.get(0).get(i_changeTypeIndex).size() != 0) {
            labelText = new StringBuilder("Previous commit SHA1: " + i_firstPrevCommit + ":\n");
            for (Map.Entry<String, String> keyVal : diff.get(0).get(i_changeTypeIndex).entrySet()) {
                labelText.append(keyVal.getKey() + "->" + keyVal.getValue() + "\n");
            }
            if (isThereSecondPrevCommit) {
                labelText.append(i_secondPrevCommit + ":\n");
                for (Map.Entry<String, String> keyVal : diff.get(1).get(i_changeTypeIndex).entrySet()) {
                    labelText.append(keyVal.getKey() + "->" + keyVal.getValue() + "\n");
                }
            }
        } else {
            labelText = new StringBuilder();
        }
        if (i_changeTypeIndex == EngineUtils.Diff.CHANGED.ordinal())
            changedItemsHistoryList.setValue(labelText.toString());

        if (i_changeTypeIndex == EngineUtils.Diff.DELETED.ordinal())
            deletedItemsHistoryList.setValue(labelText.toString());

        if (i_changeTypeIndex == EngineUtils.Diff.ADDED.ordinal())
            addedItemsHistoryList.setValue(labelText.toString());

    }



    @FXML
    private void initialize() {
        commitBtn.disableProperty().bind(isRepositoryLoaded.not());
        pushBtn.disableProperty().bind(isRemoteTracking.not());
        pullBtn.disableProperty().bind(isRemoteTracking.not());
        branchBtn.disableProperty().bind(isRepositoryLoaded.not());
        openChangesBtn.visibleProperty().bind(isRepositoryLoaded);
        repoName.textProperty().bind(currentRepositoryName);
        fileContentDisplay.textProperty().bind(currentContentDisplayed);
        headBranch.textProperty().bind(Bindings.concat("Head:").concat( headBranchName));
        userNameLabel.textProperty().bind(Bindings.concat("Hello ").concat( userName));
        changedItems.textProperty().bind(changedItemsList);
        changedItems.visibleProperty().bind(isRepositoryLoaded);
        deletedItems.textProperty().bind(deletedItemsList);
        deletedItems.visibleProperty().bind(isRepositoryLoaded);
        addedItems.textProperty().bind(addedItemsList);
        addedItems.visibleProperty().bind(isRepositoryLoaded);
        deletedItemsHistory.textProperty().bind(deletedItemsHistoryList);
        deletedItemsHistory.visibleProperty().bind(isRepositoryLoaded);
        changedItemsHistory.textProperty().bind(changedItemsHistoryList);
        changedItemsHistory.visibleProperty().bind(isRepositoryLoaded);
        addedItemsHistory.textProperty().bind(addedItemsHistoryList);
        addedItemsHistory.visibleProperty().bind(isRepositoryLoaded);
        cloneBtn.disableProperty().bind(Bindings.or(isRepositoryLoaded.not(),isRemoteTracking));
        fetchBtn.disableProperty().bind(Bindings.and(isRemoteTracking.not(), isRepositoryLoaded.not()));
//        resetButton.disableProperty().bind(sha1Selected.not());
        rebaseBranchbtn.disableProperty().bind(isRepositoryLoaded.not());
        commitTreeScrollPane.setFitToHeight(true);
        commitTreeScrollPane.setFitToWidth(true);
        pullBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("icons/pull.png"))));
        pushBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("icons/push.png"))));
        cloneBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("icons/clone.png"))));
        commitBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("icons/commit.png"))));
        branchBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("icons/branch.png"))));
        rebaseBranchbtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("icons/reset.png"))));
        pushBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("icons/push.png"))));
        userBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("icons/user.png"))));
        repositoryMenu.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("icons/repository.png"))));
        fetchBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("icons/fetch.png"))));







    }


    @FXML
    public void tabSelectionEventHandler(javafx.event.ActionEvent event) {
        if (event.getSource() == historyBtn) {
            historyTab.toFront();
            sha1Selected.set(false);
            selectedSha1.set("");
        } else if (event.getSource() == statusBtn) {
            statusTab.toFront();

        }
    }


    public void setPrimaryStage(Stage i_primaryStage) {
        primaryStage = i_primaryStage;
    }

    public void setEngine(MainEngine i_mainEngine) {
        engine = i_mainEngine;
    }


    @FXML
    public void loadXMLAction() {
        //TODO: add a repository validation and return an error popup if the file is not valid
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("text files", "*.xml"));
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile == null) {
            return;
        }

        String absolutePath = selectedFile.getAbsolutePath();
        UIadapter uiadapter = createUIAdapter();
        engine.loadRepoFromXML(uiadapter, absolutePath);
        isRepositoryLoaded.set(true);

    }
    public void displayCommitContent(String content){
        this.selectedCommitFileContent.setText("");
        this.selectedCommitFileContent.setText(content);
    }

    @FXML
    public void loadRepoAction() {
        //TODO: add a repository validation and return an error popup if the file is not valid

        String absolutePath = directoryChooser();
        if (absolutePath == null) {
            return;
        }
        UIadapter uiadapter = createUIAdapter();
        engine.loadRepo(uiadapter, absolutePath);
        isRepositoryLoaded.set(true);

    }

    public UIadapter createUIAdapter() {
        return new UIadapter(
                singleBranch -> addSingleBranch(singleBranch),
                directory -> addTreeView(directory),
                repo -> switchRepo(repo),
                message -> displayMessage(message),
                commitObj -> finalizeCommit(commitObj),
                content -> displayFileContent(content),
                conflict -> displayConflicts(conflict),
                bool -> promptUser(bool),
                ()-> createCommitMap()

        );
    }

    @FXML
    public void setUserName() {

        try {
            getUserMessage(name -> this.userName.set(name), "Please enter your name");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @FXML
    public void initRepoAction() {
        try {

            Stage popupwindow = new Stage();
            popupwindow.initModality(Modality.APPLICATION_MODAL);
            popupwindow.setTitle("Create a new repository");
            FXMLLoader loader = new FXMLLoader();
            URL mainFXML = getClass().getResource("/components/choiceWindowComponent/choiceWindow.fxml");
            loader.setLocation(mainFXML);
            VBox root = loader.load();
            PopupController controller = loader.getController();
            controller.setPrimaryStage(popupwindow);
            controller.setMainEngine(this.engine);
            controller.setUIadapter(createUIAdapter());
            Scene scene1 = new Scene(root, 400, 300);
            popupwindow.setScene(scene1);
            popupwindow.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String directoryChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select directory");
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory == null) {
            return null;
        }
        return selectedDirectory.getAbsolutePath();
    }

    public void displayMessage(String message) {
        try {
            Stage errorWindow = new Stage();
            errorWindow.initModality(Modality.APPLICATION_MODAL);
            errorWindow.setTitle("System message");
            FXMLLoader loader = new FXMLLoader();
            URL mainFXML = getClass().getResource("/components/messageWindow/messageWindowComponent.fxml");
            loader.setLocation(mainFXML);
            VBox root = loader.load();
            MessageWindowController win = loader.getController();
            win.setMessage(message);
            win.setReturnStage(errorWindow);
            Scene scene1 = new Scene(root, 400, 200);
            errorWindow.setScene(scene1);
            errorWindow.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void commitAction() {
        engine.commit(this.currentRepositoryPath.getValue(), commitObj -> finalizeCommit(commitObj), Optional.ofNullable(() -> displayMessage("Nothing to commit")));
        currentContentDisplayed.set("");
    }

    public void finalizeCommit(CommitObj commitObj) {
        try {
            getUserMessage(msg -> commitObj.setCommitMessage(msg), "Please give a short commit message");
            engine.finalizeCommit(commitObj, commitObj.mapOfdif, this.currentRepositoryPath.getValue());
            Platform.runLater(() -> {
                deletedItemsList.set("");
                addedItemsList.set("");
                changedItemsList.set("");
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getUserMessage(Consumer<String> getMessage, String title) throws IOException {
        Stage messageGetter = new Stage();
        messageGetter.initModality(Modality.APPLICATION_MODAL);
        messageGetter.setTitle("message");
        FXMLLoader loader = new FXMLLoader();
        URL mainFXML = getClass().getResource("/components/userMessageGetterComponent/userMessageGetter.fxml");
        loader.setLocation(mainFXML);
        VBox root = loader.load();
        UserInputGetter win = loader.getController();
        win.setOnFinish(getMessage);
        win.setTitle(title);
        win.setStage(messageGetter);
        Scene scene1 = new Scene(root, 400, 200);
        messageGetter.setScene(scene1);
        messageGetter.showAndWait();
    }


    public void displayOpenChanges(CommitObj obj) {
        Platform.runLater(() -> {

            Map<String, String> deleted = obj.deleted, added = obj.added, changed = obj.changed;

            List<String> d = new ArrayList<>(deleted.keySet());
            List<String> a = new ArrayList<>(added.keySet());
            List<String> c = new ArrayList<>(changed.keySet());


            deletedItemsList.set(String.join("\n", d));
            addedItemsList.set(String.join("\n", a));
            changedItemsList.set(String.join("\n", c));
        });
    }


    public void calculateOpenChanges() {
        try {
            engine.viewOpenChanges(this.currentRepositoryPath.getValue(), this::displayOpenChanges, t -> addTreeView(t), createUIAdapter());
            currentContentDisplayed.set("");
        } catch (IOException e) {
            displayMessage(e.getMessage());
        }
    }

    @FXML
    public void createNewBranchAction() {
        try {
            getUserMessage(name -> {
                engine.createNewBranch(name, msg -> displayMessage(msg), this.currentRepositoryPath.getValue());
                if (!branches.containsKey(name)) {
                    branches.put(name, branches.get(this.headBranchName.get()));
                    addSingleBranch(name);
                }
            }, "Please enter a new branch name");

        } catch (IOException e) {
            displayMessage("An error has occured");
        }

    }

    public void deleteBranch(String branchName) {
        Platform.runLater(() -> {
            if (FileUtils.getFile(this.currentRepositoryPath.getValue() + BRANCHES + branchName).delete()) {
                displayMessage("Branch has been deleted");
                branchesMenuPane.getChildren().removeIf(i -> i.getId().equals(branchName));
                branches.remove(branchName);
            } else displayMessage("Something went wrong");
        });
    }


    public Node addSingleBranch(String branch) {
        try {
            FXMLLoader loader = new FXMLLoader();
            URL mainFXML = getClass().getResource("/components/branchButton/branchComponent.fxml");
            loader.setLocation(mainFXML);
            Node singleBranch = loader.load();
            SingleBranchController singleBranchController = loader.getController();
            singleBranchController.setWord(branch);
            singleBranchController.setCheckout(
                    i -> {if(branch.contains("/"))createRTB(i);
                    else checkOutEvent(i);});
            singleBranchController.setDelete(i -> deleteBranch(i));
            singleBranchController.setMerge(i -> mergeAction(i));
            singleBranchController.setRebaseBranch(i->resetToBranch(i));
            branchesMenuPane.getChildren().add(singleBranch);
            return singleBranch;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @FXML
    private void resetBranchAction() throws IOException {
       resetBranch(selectedSha1.getValue());

    }
    private void resetToBranch(String name)  {
        if(name.contains("/"));
        name= remoteRepoName+name;
        try {
            resetBranch(branches.get(name));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void resetBranchButtonAction() throws IOException {
       SimpleStringProperty Sha1= new SimpleStringProperty("");
        getUserMessage(sha1-> Sha1.set(sha1), "Please enter a commit SHA1");
        resetBranch(Sha1.get());
    }
    private void resetBranch(String BranchSha1) throws IOException {
        engine.promptCommit(currentRepositoryPath.getValue(),this::promptUser,this::finalizeCommit,Optional.ofNullable(()-> {
            try {
                engine.resetBranch(BranchSha1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }), Optional.empty() );
        calculateOpenChanges();
    }





    private void createRTB(String i) {
        SimpleBooleanProperty userAnswer= new SimpleBooleanProperty(false);
        displayPrompt(userAnswer,"Switching to remote branch is not allowed. Would you like to create a remote tracking branch instead?");
        if (userAnswer.getValue()==false) return;
        else {
            String sha1= engine.createRTB(i);
            if (!sha1.equals("")){
            branches.put(i, sha1);
            addSingleBranch(i);}
            return;
        }
    }


    public void addTreeView(TreeItem directory) {
        Platform.runLater(() -> {
            TreeViewContainer.getChildren().clear();
            FolderTreeView.setRoot(directory);
            TreeViewContainer.getChildren().add(FolderTreeView);
        });
    }
    public void addCommitTreeView(TreeItem directory){
        Platform.runLater(()->
        {
            commitContentContainer.getChildren().clear();
            commitFolderTree.setRoot(directory);
            commitContentContainer.getChildren().add(commitFolderTree);


        });
    }


    public void switchRepo(RepoContainer repo) {
        this.currentRepositoryPath.set(repo.getDirPath());
        this.currentRepositoryName.set(repo.getDirName());
        this.headBranchName.set(repo.getHeadBranch());
        this.WCmap = repo.getWC();
        this.branches = repo.getBranches();
        if (repo.isRemote()) {
            this.remoteRepoPath = repo.getRemoteRef();
            this.remoteRepoName = repo.getRemoteRepoName();
            this.isRemoteTracking.setValue(true);
        }
        else {isRemoteTracking.setValue(false);}
        branchesMenuPane.getChildren().clear();
        currentContentDisplayed.set("");
        calculateOpenChanges();

    }

    public void displayCommitFileContent(String content)
    {

    }
    public void setWCmap(Map<String, List<FolderItem>> map) {
        this.WCmap = map;
    }

    public void checkOutEvent(String branchName) {
        try {
            engine.promptCommit(this.currentRepositoryPath.getValue(), this::promptUser, this::finalizeCommit, Optional.ofNullable(() ->
            {
                try {
                    engine.checkOut(branchName, this.currentRepositoryPath.getValue(), createUIAdapter(), () -> {
                                branchesMenuPane.getChildren().removeIf(i -> i.getId().equals(branchName));
                                addSingleBranch(this.headBranchName.getValue());
                                this.headBranchName.set(branchName);
                                currentContentDisplayed.set("");
                            }
                    );
                } catch (IOException e) {
                    displayMessage("Something went wrong");
                }
            }), Optional.empty());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void displayFileContent(String content) {
        this.currentContentDisplayed.set(content);
        //this.fileContentDisplay.setText(content);
    }

    public void mergeAction(String name) {
        try {
            engine.promptCommit(this.currentRepositoryPath.getValue(), this::promptUser, this::finalizeCommit, Optional.ofNullable(() -> {

                try {
                    engine.mergeAction(name, this.headBranchName.getValue(), createUIAdapter());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }), Optional.empty());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public void displayPrompt(SimpleBooleanProperty userAnswer, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setContentText(message);
        alert.initStyle(StageStyle.UTILITY);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            userAnswer.setValue(true);
        } else {
            userAnswer.setValue(false);

        }
    }

    public void promptUser(SimpleBooleanProperty userAnswer) {
        displayPrompt(userAnswer, "Open changes have been detected. Would you like to commit them before proceeding?" +
                "(unsaved changes will be discarded)");
    }

    public void displayConflict(String ours, String theirs, String parent, Consumer<String> setContent) {
        try {
            Stage merger = new Stage();
            merger.initModality(Modality.APPLICATION_MODAL);
            merger.setTitle("message");
            FXMLLoader loader = new FXMLLoader();
            URL mainFXML = getClass().getResource("/components/Merger/conflictResolver.fxml");
            loader.setLocation(mainFXML);
            GridPane root = loader.load();
            MergeController win = loader.getController();
            win.setMerger(setContent);
            win.setOurs(ours);
            win.setStage(merger);
            win.setTheirs(theirs);
            win.setParent(parent);
            Scene scene1 = new Scene(root, 600, 450);
            merger.setScene(scene1);
            merger.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void displayConflicts(Map<String, ConflictObj> coflict) {
        String filePath = currentRepositoryPath.getValue() + "\\.magit\\objects\\";
        for (ConflictObj i : coflict.values()) {
            try {
                displayConflict(
                        (i.getOursSha1().equals("") ? "" : EngineUtils.listToString(EngineUtils.getZippedFileLines(filePath + i.getOursSha1() + ".zip"), "\n")),
                        (i.getTheirsSha1().equals("") ? "" : EngineUtils.listToString(EngineUtils.getZippedFileLines(filePath + i.getTheirsSha1() + ".zip"), "\n")),
                        (i.getancestorsSha1().equals("") ? "" : EngineUtils.listToString(EngineUtils.getZippedFileLines(filePath + i.getancestorsSha1() + ".zip"), "\n")),
                        c -> i.setContent(c)
                );


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @FXML
    public void cloneAction() throws IOException {
        SimpleBooleanProperty userReply = new SimpleBooleanProperty(false);
        SimpleStringProperty dirName = new SimpleStringProperty("");
        displayPrompt(userReply, "Would you like to clone this repository? Press OK to continue and provide a destination path and directory name.");
        if (userReply.getValue() == false) return;
        String absolutePath = directoryChooser();
        if (absolutePath == null) {
            return;
        }

        getUserMessage(s -> dirName.setValue(s), "Please provide a directory name");

        engine.cloneRepo(this.currentRepositoryPath.getValue(), absolutePath, dirName.getValue());
    }

    public void fetchAction() throws IOException {
//        engine.promptCommit(currentRepositoryPath.getValue(), this::promptUser, this::finalizeCommit, Optional.ofNullable(() -> engine.fetch()), Optional.empty());
//        calculateOpenChanges();
        engine.fetch();
    }
    @FXML
    public void pullAction() throws IOException {
        if(branches.containsKey(remoteRepoName+"/"+headBranchName.getValue()))
            engine.promptCommit(currentRepositoryPath.getValue(),this::promptUser,this::finalizeCommit,Optional.ofNullable(()-> {
                try {
                    engine.pull();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }),Optional.empty());
        else displayMessage("The head branch is not a remote tracking branch");
    }
    @FXML
    public void pushAction() throws IOException {
        if(branches.containsKey(remoteRepoName+"/"+headBranchName.getValue()))
            engine.push();
        else displayMessage("The head branch is not a remote tracking branch");

    }

}