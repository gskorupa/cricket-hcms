package pl.experiot.hcms.app.ports.driving;

import java.util.HashMap;
import java.util.List;

import pl.experiot.hcms.app.logic.Site;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;

public interface ForChangeWatcherIface {
    void setLoader(ForDocumentsLoaderIface loader);
    List<ForChangeWatcherIface> getInstances(HashMap<String,Site> siteMap);
}