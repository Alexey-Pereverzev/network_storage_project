package server.authentication;

import java.sql.*;

public class DBAuthenticationService implements AuthenticationService {

    private Connection connection;
    private Statement stmt;
    private ResultSet rs;

    @Override
    public boolean authUserByLoginAndPassword(String login, String password) throws SQLException {
                    //  авторизация по логину и паролю, возвращает успешна ли авторизация
        rs = stmt.executeQuery(String.format("SELECT * FROM auth WHERE login = '%s'", login));
        if (rs.isClosed()) {
            return false;
        }
        String username = rs.getString("login");
        String passwordDB = rs.getString("password");

        if (username!=null && passwordDB != null) {
            return passwordDB.equals(password) && username.equals(login);
        } else {
            return false;
        }
    }

    @Override
    public void startAuthentication() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:src/main/resources/db/authinfo.db");
        stmt = connection.createStatement();
    }

    @Override
    public void endAuthentication() throws SQLException {
        connection.close();
    }


}
