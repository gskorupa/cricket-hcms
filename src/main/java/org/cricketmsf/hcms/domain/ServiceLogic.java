package org.cricketmsf.hcms.domain;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Executors;

import org.cricketmsf.hcms.application.out.DocumentRepositoryLoader;
import org.cricketmsf.hcms.application.out.FolderWatcher;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class ServiceLogic {

    @ConfigProperty(name = "document.folders.root")
    String root;

    @Inject
    Logger logger;

    @Inject
    DocumentRepositoryLoader loader;

    void onStart(@Observes StartupEvent ev) {
        Executors.newSingleThreadExecutor().execute(new FolderWatcher(root, loader));
    }

    

}
