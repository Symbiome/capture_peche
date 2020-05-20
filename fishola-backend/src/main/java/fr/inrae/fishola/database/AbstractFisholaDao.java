package fr.inrae.fishola.database;

import fr.inrae.fishola.FisholaConfiguration;
import io.agroal.api.AgroalDataSource;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static javax.transaction.Transactional.TxType.MANDATORY;

@Transactional(MANDATORY)
public abstract class AbstractFisholaDao {

    @Inject
    protected AgroalDataSource dataSource;

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

    protected <R> R withContext(JooqContext<R> work) {
        DSLContext context = DSL.using(dataSource, SQLDialect.POSTGRES);
        R result = work.execute(context);
        return result;
    }

    protected <R> void withContextNoResult(JooqContextNoResult<R> work) {
        DSLContext context = DSL.using(dataSource, SQLDialect.POSTGRES);
        work.execute(context);
    }

    protected <R> R withConfiguration(JooqConfiguration<R> work) {
        Configuration configuration = new DefaultConfiguration().set(dataSource).set(SQLDialect.POSTGRES);
        R result = work.execute(configuration);
        return result;
    }

    protected <R, D> R withDao(Class<D> daoClass, JooqDao<D, R> work) {
        try {
            Configuration configuration = new DefaultConfiguration().set(dataSource).set(SQLDialect.POSTGRES);
            Constructor<D> constructor = daoClass.getConstructor(Configuration.class);
            D dao = constructor.newInstance(configuration);
            R result = work.execute(dao);
            return result;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException eee) {
            throw new RuntimeException("Unable to treat jOOQ dao work", eee);
        }
    }

    protected <D> void withDaoNoResult(Class<D> daoClass, JooqDaoNoResult<D> work) {
        try {
            Configuration configuration = new DefaultConfiguration().set(dataSource).set(SQLDialect.POSTGRES);
            Constructor<D> constructor = daoClass.getConstructor(Configuration.class);
            D dao = constructor.newInstance(configuration);
            work.execute(dao);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException eee) {
            throw new RuntimeException("Unable to treat jOOQ dao work", eee);
        }
    }

}
