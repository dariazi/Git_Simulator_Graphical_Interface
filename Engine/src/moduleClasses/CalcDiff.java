package moduleClasses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalcDiff {
    public Map<String, String> added;
    public Map<String, String> changed;
    public Map<String, String> deleted;
    public Map<String, List<FolderItem>> mapOfdif;


    public CalcDiff()
    {
        added= new HashMap<>();
        changed= new HashMap<>();
        deleted= new HashMap<>();
        mapOfdif= new HashMap<>();
    }
}
