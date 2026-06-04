package pl.experiot.hcms.app.logic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import jakarta.ws.rs.core.Response;
import pl.experiot.hcms.adapters.driven.auth.SignomixAuthClient;
import pl.experiot.hcms.app.logic.dto.User;

@ApplicationScoped
public class TokenCache {

    @Inject
    @RestClient
    SignomixAuthClient authClient;

    @CacheResult(cacheName = "token-cache")
    public User getUser(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            Response response = authClient.getUser(token);
            if (response.getStatus() == 200) {
                return response.readEntity(User.class);
            }
        } catch (Exception e) {
            // ignore -- caller treats null as unauthorized
        }
        return null;
    }

    @CacheInvalidateAll(cacheName = "token-cache")
    public void clear() {
    }
}
