package pl.experiot.hcms.app.ports.driving;

import java.util.List;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;

public interface ForChangeWatcherIface {
    void setLoader(ForDocumentsLoaderIface loader);

    public String getNameplate();

    List<ForChangeWatcherIface> getInstances();
    
    /**
     * Stops the watcher instance gracefully.
     */
    default void stop() {
        // Default no-op implementation for backward compatibility
    }
}
