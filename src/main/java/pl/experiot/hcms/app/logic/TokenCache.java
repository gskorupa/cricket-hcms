package pl.experiot.hcms.app.logic;

import java.util.HashMap;

import jakarta.enterprise.context.ApplicationScoped;
import pl.experiot.hcms.app.logic.dto.User;

@ApplicationScoped
public class TokenCache {

    private static final int MAX_TOKENS = 500;

    private final java.util.Map<String, User> tokens = java.util.Collections.synchronizedMap(
        new java.util.LinkedHashMap<String, User>(16, 0.75f, true) {
            private static final long serialVersionUID = 1L;
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<String, User> eldest) {
                return size() > MAX_TOKENS;
            }
        }
    );

    public void addToken(String token, User user) {
        tokens.put(token, user);
    }

    public User getUser(String token) {
        return tokens.get(token);
    }

    public boolean containsToken(String token) {
        return tokens.containsKey(token);
    }

    public void clear() {
        tokens.clear();
    }

}
