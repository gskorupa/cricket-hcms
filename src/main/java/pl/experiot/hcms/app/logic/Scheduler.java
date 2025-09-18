package pl.experiot.hcms.app.logic;

import org.jboss.logging.Logger;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Scheduler {

    @Inject
    Logger logger;

    @Inject
    TokenCache tokenCache;

    @Inject
    DocumentLogic documentLogic;

    @Scheduled(every = "1h")
    @CacheInvalidateAll(cacheName = "token-cache")
    @CacheInvalidateAll(cacheName = "document-cache")
    @CacheInvalidateAll(cacheName = "document-list-cache")
    void clearCache() {
        tokenCache.clear();
        logger.info("Document repository size: " + documentLogic.getRepositorySize());
    }


}
