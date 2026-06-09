package com.ycz.childnotesbackend.context;

import com.ycz.childnotesbackend.model.auth.AuthUser;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public class AuthContext {

    private static final ThreadLocal<AuthUser> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setCurrentUser(AuthUser user) {
        CURRENT_USER.set(user);
    }

    public static AuthUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static Long requireCurrentUserId() {
        AuthUser user = getCurrentUser();
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
        }
        return user.getId();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
