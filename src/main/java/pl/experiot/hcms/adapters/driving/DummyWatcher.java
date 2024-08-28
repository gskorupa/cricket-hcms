package pl.experiot.hcms.adapters.driving;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pl.experiot.hcms.app.logic.Site;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;
import pl.experiot.hcms.app.ports.driving.ForChangeWatcherIface;

public class DummyWatcher implements ForChangeWatcherIface, Runnable {

    @Override
    public void run() {
    }

    @Override
    public void setLoader(ForDocumentsLoaderIface loader) {
    }

    @Override
    public List<ForChangeWatcherIface> getInstances(HashMap<String,Site> siteMap ) {
        ArrayList<ForChangeWatcherIface> instances=new ArrayList<>();
        instances.add(this);
        return instances;
    }
    
}
