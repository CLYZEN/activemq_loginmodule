package kr.damda;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class DatabaseLoginModule implements LoginModule {

    private CallbackHandler callbackHandler;
    private boolean loginSucceeded = false;
    private String username;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
        Map<String, ?> sharedState, Map<String, ?> options) {
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", true);

        try {
            callbackHandler.handle(callbacks);
            String username = ((NameCallback) callbacks[0]).getName();
            char[] password = ((PasswordCallback) callbacks[1]).getPassword();

            if (username == null || password == null) {
                throw new LoginException("Username or password is null");
            }

            // Validate the username and password against the database
            if (validateAgainstDatabase(username, new String(password))) {
                loginSucceeded = true;
                this.username = username;
                return true;
            } else {
                throw new LoginException("Authentication failed");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new LoginException(e.getMessage());
        }
    }

    private boolean validateAgainstDatabase(String username, String password) {
        // Load the MariaDB driver
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        // Replace with your actual database connection and query logic
        try (Connection connection = DriverManager.getConnection("jdbc:mariadb://192.168.167.221:3306/iot", "root", "root1234");
            PreparedStatement statement = connection.prepareStatement(
                "SELECT auth_pwd FROM iot_mq_auth WHERE auth_id = ?")) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String storedPassword = resultSet.getString("auth_pwd");
                return storedPassword.equals(
                    password); // You may want to hash passwords and compare hashes
            } else {
                System.out.println("No user found with username: " + username);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean commit() throws LoginException {
        return loginSucceeded;
    }

    @Override
    public boolean abort() throws LoginException {
        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        return false;
    }
}