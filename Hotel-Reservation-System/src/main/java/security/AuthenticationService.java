package security;

import java.util.Optional;

/**
 * Very simple stub authentication service.
 * Replace with real logic later if needed.
 */
public class AuthenticationService {

    private AdminUser currentUser;

    public Optional<AdminUser> authenticate(String username, String password) {
        // TODO: implement real authentication.
        // For now, always fail login so the app compiles safely.
        return Optional.empty();
    }

    public AdminUser getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(AdminUser currentUser) {
        this.currentUser = currentUser;
    }
}
