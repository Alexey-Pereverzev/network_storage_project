package server.authentication;

import server.HashEncoder;

import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class DBAuthenticationService implements AuthenticationService {
    //  сервис аутентификации через БД с хешированными паролями
    private final HashEncoder hashEncoder;
    private Connection connection;
    private Statement stmt;

    public DBAuthenticationService(HashEncoder hashEncoder) {
        this.hashEncoder = hashEncoder;
    }

    @Override
    public String authUserByLoginAndPassword(String login, String password) throws SQLException, NoSuchAlgorithmException {
        //  авторизация по логину и паролю, возвращает "ок", если авторизация успешна или сообщение об ошибке
        ResultSet rs = stmt.executeQuery(String.format("SELECT * FROM auth WHERE login = '%s'", login));

        if (rs.isClosed()) {
            return "Ошибка авторизации";
        }
        String username = rs.getString("login");
        String passwordDB = rs.getString("password");

        if (username != null && passwordDB != null) {
            if (passwordDB.equals(hashEncoder.getHash(password)) && username.equals(login)) {
                return "ok";
            } else {
                return "Неверное имя пользователя и/или пароль";
            }
        } else {
            return "Ошибка авторизации";
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
