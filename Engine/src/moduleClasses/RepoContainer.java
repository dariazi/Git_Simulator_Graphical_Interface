package moduleClasses;

import java.util.List;
import java.util.Map;

public class RepoContainer {
    private String headBranch;
    private String dirPath;
    private String dirName;
    private Map<String, List<FolderItem>> WC;
    private Map <String, String> branches;
    private String rootDirSha1;
    private boolean isRemote;
    private String remoteRepoLocation;
    private String remoteRepoName;


    public RepoContainer(String head, Map<String, List<FolderItem>> map,Map <String, String> branches,String rootDirSHA1,String address, String name) {
    this.headBranch=head;
        this.dirPath=address;
        this.dirName=name;
        this.WC=map;
        this.branches=branches;
        this.rootDirSha1=rootDirSHA1;
    }
    public void setRemoteRef(String locaction, String name){
        this.isRemote=true;
        this.remoteRepoLocation= locaction;
        this.remoteRepoName= name;
    }
    public String getHeadBranch(){return this.headBranch;}
    public String getDirName(){return this.dirName;}
    public  Map<String, List<FolderItem>> getWC(){return this.WC;}
    public Map <String, String> getBranches(){return this.branches;}
    public  String getDirPath(){return  this.dirPath;}
    public String getRootDir(){return this.rootDirSha1;}
    public String getRemoteRef(){
        return remoteRepoLocation;
    }
    public String getRemoteRepoName(){
        return remoteRepoName;
    }
    public Boolean isRemote(){
        return isRemote;
    }





}
