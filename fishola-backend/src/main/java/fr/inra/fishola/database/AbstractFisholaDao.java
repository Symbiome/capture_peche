package fr.inra.fishola.database;

import fr.inra.fishola.FisholaConfiguration;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class AbstractFisholaDao {

    @Inject
    protected FisholaConfiguration config;

    public interface JooqContext<T> {
        T execute(DSLContext context);
    }

    public interface JooqContextNoResult<T> {
        void execute(DSLContext context);
    }

    public interface JooqConfiguration<T> {
        T execute(Configuration configuration);
    }

    public interface JooqDao<D, T> {
        T execute(D dao);
    }

    public interface JooqDaoNoResult<D> {
        void execute(D dao);
    }

    protected Connection newConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(config.getJdbcUrl(), config.getJdbcUser(), config.getJdbcPassword());
        return conn;
    }

    protected <R> R withContext(JooqContext<R> work) {
        try (Connection connection = newConnection()) {
            DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            R result = work.execute(context);
            return result;
        } catch (SQLException sqle) {
            throw new RuntimeException("Unable to treat jOOQ context work", sqle);
        }
    }

    protected <R> void withContextNoResult(JooqContextNoResult<R> work) {
        try (Connection connection = newConnection()) {
            DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            work.execute(context);
        } catch (SQLException sqle) {
            throw new RuntimeException("Unable to treat jOOQ context work", sqle);
        }
    }

    protected <R> R withConfiguration(JooqConfiguration<R> work) {
        try(Connection connection = newConnection()) {
            Configuration configuration = new DefaultConfiguration().set(connection).set(SQLDialect.POSTGRES);
            R result = work.execute(configuration);
            return result;
        } catch (SQLException sqle) {
            throw new RuntimeException("Unable to treat jOOQ config work", sqle);
        }
    }

    protected <R, D> R withDao(Class<D> daoClass, JooqDao<D, R> work) {
        try(Connection connection = newConnection()) {
            Configuration configuration = new DefaultConfiguration().set(connection).set(SQLDialect.POSTGRES);
            Constructor<D> constructor = daoClass.getConstructor(Configuration.class);
            D dao = constructor.newInstance(configuration);
            R result = work.execute(dao);
            return result;
        } catch (SQLException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException eee) {
            throw new RuntimeException("Unable to treat jOOQ dao work", eee);
        }
    }

    protected <D> void withDaoNoResult(Class<D> daoClass, JooqDaoNoResult<D> work) {
        try(Connection connection = newConnection()) {
            Configuration configuration = new DefaultConfiguration().set(connection).set(SQLDialect.POSTGRES);
            Constructor<D> constructor = daoClass.getConstructor(Configuration.class);
            D dao = constructor.newInstance(configuration);
            work.execute(dao);
        } catch (SQLException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException eee) {
            throw new RuntimeException("Unable to treat jOOQ dao work", eee);
        }
    }

}
