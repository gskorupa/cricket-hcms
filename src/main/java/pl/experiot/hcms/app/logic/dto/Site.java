package pl.experiot.hcms.app.logic.dto;

import java.util.HashSet;

public class Site {
    public String name;
    public String indexFile;
    public String assetsPath;
    public String watchedFile;
    public HashSet<String> excludedPaths;
    public String hcmsServiceLocation;
    public String hcmsFileApiPath;
}
