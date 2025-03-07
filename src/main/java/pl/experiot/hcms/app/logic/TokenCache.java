package pl.experiot.hcms.app.logic;

import java.util.HashMap;

import jakarta.enterprise.context.ApplicationScoped;
import pl.experiot.hcms.app.logic.dto.User;

@ApplicationScoped
public class TokenCache {
    private HashMap<String, User> tokens = new HashMap<>();

    private HashMap<String, User> getTokenMap() {
        if(tokens == null) {
            tokens = new HashMap<>();
        }
        return tokens;
    }

    public void addToken(String token, User user) {
        getTokenMap().put(token, user);
    }

    public User getUser(String token) {
        return getTokenMap().get(token);
    }
    
    public boolean containsToken(String token) {
        return getTokenMap().containsKey(token);
    }


}
