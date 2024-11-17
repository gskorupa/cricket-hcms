package pl.experiot.hcms.app.ports.driving;

import java.util.HashMap;
import java.util.List;

import pl.experiot.hcms.app.logic.dto.Site;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;

public interface ForChangeWatcherIface {
    void setLoader(ForDocumentsLoaderIface loader);
    public String getNameplate();
    List<ForChangeWatcherIface> getInstances();
}