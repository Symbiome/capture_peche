package fr.inra.fishola.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class AbstractFisholaDao {

    protected Connection newConnection() throws SQLException {

        String url = "jdbc:postgresql://172.17.0.2/fishola";
        String user = "postgres";
        String password = null;

        Connection conn = DriverManager.getConnection(url, user, password);
        return conn;
    }

}
