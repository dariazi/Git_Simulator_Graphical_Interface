package moduleClasses;

import puk.team.course.magit.ancestor.finder.CommitRepresentative;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class CommitObj extends CalcDiff implements CommitRepresentative {

    public ArrayList<String> PreviousCommitsSha1;
    public ArrayList<String> PreviousCommitsID;
    public String rootFolderID;
    public String rootDirSha1;
    public String dateCreated;
    public String commitMessage;
    private String m_submitterName;
    private String commitSha1;
    private List<String> branchesPointToThisCommit = new ArrayList<>();

    public CommitObj() {
        PreviousCommitsSha1 = new ArrayList(2);
        PreviousCommitsSha1.add("");
        PreviousCommitsSha1.add("");
        PreviousCommitsID = new ArrayList(2);
        PreviousCommitsID.add("");
        PreviousCommitsID.add("");
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-hh:mm:ss:sss");
        Date date = new Date();
        dateCreated = dateFormat.format(date);
    }

    public CommitObj(String i_dateCreated, String i_commitMessage, String i_submitterName, ArrayList i_previousCommit, String i_rootFolderID) {
        dateCreated = i_dateCreated;
        commitMessage = i_commitMessage;
        m_submitterName = i_submitterName;
        PreviousCommitsID = i_previousCommit;
        rootFolderID = i_rootFolderID;
        PreviousCommitsSha1 = new ArrayList(2);
        PreviousCommitsSha1.add("");
        PreviousCommitsSha1.add("");
    }

    public CommitObj(String i_dateCreated, String i_commitMessage, String i_submitterName, String i_rootFolderSha1, ArrayList i_PreviousCommitsSha1, String i_commitSha1, List i_branchesPointToThisCommit) {
        dateCreated = i_dateCreated;
        commitMessage = i_commitMessage;
        m_submitterName = i_submitterName;
        rootDirSha1 = i_rootFolderSha1;
        PreviousCommitsSha1 = i_PreviousCommitsSha1;
        if (i_PreviousCommitsSha1.size()==0)PreviousCommitsSha1.add("");
        if(i_PreviousCommitsSha1.size()==1) PreviousCommitsSha1.add("");
        commitSha1 = i_commitSha1;
        if (i_branchesPointToThisCommit != null) {
            branchesPointToThisCommit = i_branchesPointToThisCommit;
        }
    }

    public List<String> getBranchesPointToThisCommit() {
        return branchesPointToThisCommit;
    }

    public void addPointingBranch(String i_branch) {
        branchesPointToThisCommit.add(i_branch);
    }

    public String getCommitSha1() {
        return commitSha1;
    }

    public void setPreviousCommitsSha1(String[] precedingCommit) {
        int i = 0;
        while (i < precedingCommit.length) {
            PreviousCommitsSha1.set(i, precedingCommit[i]);
            i++;
        }


    }

    public void setCommitSHA1(String commitSHA1) {
        rootDirSha1 = commitSHA1;
    }

    public void setPrevSha1(String sha1) {
        if (PreviousCommitsSha1.get(0).equals("")) PreviousCommitsSha1.set(0, sha1);
        else {
            PreviousCommitsSha1.set(1, sha1);
        }
    }

    public void setPrevID(String sha1) {
        if (PreviousCommitsID.get(0).equals("")) PreviousCommitsID.set(0, sha1);
        else {
            PreviousCommitsID.set(1, sha1);
        }
    }

    public String getUserName() {
        return m_submitterName;
    }

    public void setUserName(String i_userNAme) {
        m_submitterName = i_userNAme;
    }

    public String getRootDirSha1() {
        return rootDirSha1;
    }

    public void setCommitMessage(String message) {
        this.commitMessage = message;
    }

    @Override
    public String toString() {
        return rootDirSha1 + "\n" + String.join(",", PreviousCommitsSha1.stream().collect(Collectors.toList())) + "\n" + m_submitterName + "\n" + dateCreated + "\n" + commitMessage + "\ncommit";


    }

    @Override
    public String getSha1() {
        return commitSha1;
    }

    public void setSha1(String sha1) {
        this.commitSha1 = sha1;
    }

    @Override
    public String getFirstPrecedingSha1() {
        return PreviousCommitsSha1.get(0);
    }

    @Override
    public String getSecondPrecedingSha1() {
        return PreviousCommitsSha1.get(1);
    }

}