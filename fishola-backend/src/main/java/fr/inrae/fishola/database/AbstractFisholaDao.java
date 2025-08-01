package fr.inrae.fishola.database;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import fr.inrae.fishola.FisholaConfiguration;
import io.agroal.api.AgroalDataSource;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static jakarta.transaction.Transactional.TxType.MANDATORY;


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

    protected DSLContext newContext() {
        final DSLContext result = DSL.using(dataSource, SQLDialect.POSTGRES);
        return result;
    }

    protected <R> R withContext(JooqContext<R> work) {
        DSLContext context = newContext();
        R result = work.execute(context);
        return result;
    }

    protected <R> void withContextNoResult(JooqContextNoResult<R> work) {
        DSLContext context = newContext();
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
