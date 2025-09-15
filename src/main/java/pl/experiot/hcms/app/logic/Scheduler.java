package pl.experiot.hcms.app.logic;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Scheduler {

    @Inject
    TokenCache tokenCache;

    @Scheduled(every = "1h")
    void clearCache() {
        tokenCache.clear();
    }


}
