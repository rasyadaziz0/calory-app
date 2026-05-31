package Auth.repository;

import Auth.model.User;
import java.util.*;

public interface UserRepository {
    User saveUpdate(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(String id);
}
