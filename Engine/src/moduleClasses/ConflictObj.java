package moduleClasses;

public class ConflictObj {
    private String ancestorSha1;
    private String oursSha1;
    private String theirsSha1;
    private String content;

    public ConflictObj(String ancestor, String ours, String theirs){
        ancestorSha1= ancestor;
        oursSha1=ours;
        theirsSha1=theirs;

    }
//    public void setContent(Object product){
//        content= (String) product;
//    }
    public String getancestorsSha1(){return ancestorSha1;}
    public String getOursSha1(){return oursSha1;}
    public String getTheirsSha1(){return theirsSha1;}




//    public void setContent(Object c) {
//        content= (String) c;
//    }

    public void setContent(String  c) {
        content= c;
    }
    public String getContent(){
        return content;
    }
}
