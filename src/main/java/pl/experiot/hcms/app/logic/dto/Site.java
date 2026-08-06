package pl.experiot.hcms.app.logic.dto;

import java.util.HashSet;
import java.util.Set;

public class Site {
    public String name;
    public String indexFile;
    public String assetsPath;
    public Set<String> watchedFiles;
    public HashSet<String> excludedPaths;
    public String hcmsServiceLocation;
    public String hcmsFileApiPath;
}
