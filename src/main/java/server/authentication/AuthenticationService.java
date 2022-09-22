package server.authentication;


import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public interface AuthenticationService {

    String authUserByLoginAndPassword(String login, String password) throws SQLException, NoSuchAlgorithmException;

    void startAuthentication() throws ClassNotFoundException, SQLException;

    void endAuthentication() throws SQLException;

}

