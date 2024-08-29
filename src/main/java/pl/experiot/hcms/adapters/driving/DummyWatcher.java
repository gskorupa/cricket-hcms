package pl.experiot.hcms.adapters.driving;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;
import pl.experiot.hcms.app.ports.driving.ForChangeWatcherIface;

public class DummyWatcher implements ForChangeWatcherIface, Runnable {

    Logger logger = Logger.getLogger(DummyWatcher.class);

    public DummyWatcher() {
        logger.info("Creating DummyWatcher "+getClass().getSimpleName());
    }

    @Override
    public void run() {
    }

    @Override
    public void setLoader(ForDocumentsLoaderIface loader) {
    }

    @Override
    public List<ForChangeWatcherIface> getInstances() {
        logger.info("Creating instances of DummyWatcher "+getClass().getSimpleName());
        ArrayList<ForChangeWatcherIface> instances = new ArrayList<>();
        instances.add(new DummyWatcher());
        return instances;
    }

    @Override
    public String getNameplate() {
        return getClass().getSimpleName();
    }

}
