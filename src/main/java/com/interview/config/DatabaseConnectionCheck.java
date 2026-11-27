package com.interview.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnectionCheck {

    public static void main(String[] args) throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/kyc_practice";
        String username = "kyc_user";
        String password = "kyc_local_password";

        try (
                Connection connection =
                        DriverManager.getConnection(url, username, password);

                Statement statement = connection.createStatement();

                ResultSet result = statement.executeQuery(
                        "SELECT current_database(), current_user"
                )
        ) {
            if (!result.next()) {
                throw new IllegalStateException(
                        "Database check returned no result"
                );
            }

            System.out.println("Database: " + result.getString(1));
            System.out.println("User: " + result.getString(2));
        }
    }
}