package Auth.service;

import Auth.model.User;

public interface AuthService {
    User Login() throws AuthException;
    void Logout(User user) ;
    boolean isLoggedIn(User user) ; 
}