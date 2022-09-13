package server.authentication;


import java.sql.SQLException;

public interface AuthenticationService {

    boolean authUserByLoginAndPassword(String login, String password) throws SQLException;

    void startAuthentication() throws ClassNotFoundException, SQLException;

    void endAuthentication() throws SQLException;

}

