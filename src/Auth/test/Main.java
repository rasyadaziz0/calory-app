package Auth.test;

import Auth.config.AuthConfig;
import Auth.model.User;
import Auth.repository.UserRepository;
import Auth.repository.SupabaseUserRepository;
import Auth.service.GoogleAuthService;

public class Main {
    public static void main(String[] args) throws Exception {
        AuthConfig config = new AuthConfig();
        UserRepository repo = new SupabaseUserRepository(config);
        GoogleAuthService auth = new GoogleAuthService(config, repo);
        User user = auth.Login();
        System.out.println("User: " + user);
    }
}
