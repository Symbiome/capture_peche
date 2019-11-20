package fr.inra.fishola.database;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class AbstractFisholaDao {

    public interface JooqWork<T> {

        T execute(DSLContext context);

    }

    protected Connection newConnection() throws SQLException {

        String url = "jdbc:postgresql://172.17.0.2/fishola";
        String user = "postgres";
        String password = null;

        Connection conn = DriverManager.getConnection(url, user, password);
        return conn;
    }

    protected <R> R run(JooqWork<R> work) {
        try (Connection connection = newConnection()) {
            DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            R result = work.execute(context);
            return result;
        } catch (SQLException sqle) {
            throw new RuntimeException("Unable to treat jOOQ work", sqle);
        }
    }

}
