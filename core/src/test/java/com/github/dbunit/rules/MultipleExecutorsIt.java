package com.github.dbunit.rules;

import static com.github.dbunit.rules.EntityManagerProvider.instance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.github.dbunit.rules.connection.ConnectionHolderImpl;
import com.github.dbunit.rules.dataset.DataSetExecutor;
import com.github.dbunit.rules.dataset.DataSetModel;
import com.github.dbunit.rules.model.Follower;
import com.github.dbunit.rules.model.User;

/**
 * Created by pestano on 23/07/15.
 */

@RunWith(JUnit4.class)
public class MultipleExecutorsIt {


    private static List<DataSetExecutor> executors = new ArrayList<>(3);

    @BeforeClass
    public static void setup() {
        executors.add(DataSetExecutor.instance("executor1", new ConnectionHolderImpl(instance("executor1-pu").getConnection())));
        executors.add(DataSetExecutor.instance("executor2", new ConnectionHolderImpl(instance("executor2-pu").getConnection())));
        executors.add(DataSetExecutor.instance("executor3", new ConnectionHolderImpl(instance("executor3-pu").getConnection())));
    }

    @AfterClass
    public static void tearDown() throws SQLException {
        for (DataSetExecutor executor : executors) {
            Connection connection = executor.getConnection();
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }

    }

    @Test
    public void shouldSeedDataSetDisablingContraints() {
        for (DataSetExecutor executor : executors) {
            DataSetModel dataSetModel = new DataSetModel("datasets/yml/users.yml").disableConstraints(true);
            executor.execute(dataSetModel);
            User user = (User) EntityManagerProvider.instance(executor.getName() + "-pu").em().createQuery("select u from User u where u.id = 1").getSingleResult();
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(1);
        }

    }

    @Test
    public void shouldSeedDataSetDisablingContraintsViaStatement() {
        for (DataSetExecutor executor : executors) {
            DataSetModel dataSetModel = new DataSetModel("datasets/yml/users.yml").executeStatementsAfter(new String[]{"SET DATABASE REFERENTIAL INTEGRITY FALSE;"});
            executor.execute(dataSetModel);
            User user = (User) EntityManagerProvider.instance(executor.getName() + "-pu").em().createQuery("select u from User u where u.id = 1").getSingleResult();
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(1);
        }

    }


    @Test
    public void shouldNotSeedDataSetWithoutSequenceFilter() {
        for (DataSetExecutor executor : executors) {
            DataSetModel dataSetModel = new DataSetModel("datasets/yml/users.yml").
                useSequenceFiltering(false).
                executeStatementsAfter(new String[] { "DELETE FROM User" });//needed because other tests creates users and as the dataset is not created in this test the CLEAN is not performed
            executor.execute(dataSetModel);
            List<User> users =  EntityManagerProvider.instance(executor.getName() + "-pu").em().createQuery("select u from User u").getResultList();
            assertThat(users).isEmpty();
        }

    }

    @Test
    public void shouldSeedDataSetUsingTableCreationOrder() {
        for (DataSetExecutor executor : executors) {
            DataSetModel dataSetModel = new DataSetModel("datasets/yml/users.yml").
                tableOrdering(new String[]{"USER","TWEET","FOLLOWER"}).
                executeStatementsBefore(new String[]{"DELETE FROM FOLLOWER","DELETE FROM TWEET","DELETE FROM USER"}).//needed because other tests created user dataset
                useSequenceFiltering(false);
            executor.execute(dataSetModel);
            List<User> users =  EntityManagerProvider.instance(executor.getName() + "-pu").em().createQuery("select u from User u").getResultList();
            assertThat(users).hasSize(2);
        }

    }

    @Test
    public void shouldSeedUserDataSet() {
        for (DataSetExecutor executor : executors) {
            DataSetModel dataSetModel = new DataSetModel("datasets/yml/users.yml").
                useSequenceFiltering(true);
            executor.execute(dataSetModel);
            User user = (User) EntityManagerProvider.instance(executor.getName() + "-pu").em().createQuery("select u from User u where u.id = 1").getSingleResult();
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(1);
        }

    }

    @Test
    public void shouldLoadUserFollowers() {
        for (DataSetExecutor executor : executors) {
            DataSetModel dataSetModel = new DataSetModel("datasets/yml/users.yml");
            executor.execute(dataSetModel);
            User user = (User) EntityManagerProvider.instance(executor.getName() + "-pu").em().createQuery("select u from User u left join fetch u.followers where u.id = 1").getSingleResult();
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(1);
            assertThat(user.getTweets()).hasSize(1);
            assertEquals(user.getTweets().get(0).getContent(), "dbunit rules!");
            assertThat(user.getFollowers()).isNotNull().hasSize(1);
            Follower expectedFollower = new Follower(2,1);
            assertThat(user.getFollowers()).contains(expectedFollower);
        }

    }

    @Test
    public void shouldLoadUsersFromJsonDataset() {
        for (DataSetExecutor executor : executors) {
            DataSetModel dataSetModel = new DataSetModel("datasets/json/users.json");
            executor.execute(dataSetModel);
            User user = (User) EntityManagerProvider.instance(executor.getName() + "-pu").em().createQuery("select u from User u left join fetch u.followers where u.id = 1").getSingleResult();
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(1);
            assertThat(user.getTweets()).hasSize(1);
            assertEquals("dbunit rules json example",user.getTweets().get(0).getContent());
            assertThat(user.getFollowers()).isNotNull().hasSize(1);
            Follower expectedFollower = new Follower(2,1);
            assertThat(user.getFollowers()).contains(expectedFollower);
        }

    }

    @Test
    public void shouldLoadUsersFromXmlDataset() {
        for (DataSetExecutor executor : executors) {
            DataSetModel dataSetModel = new DataSetModel("datasets/xml/users.xml");
            executor.execute(dataSetModel);
            User user = (User) EntityManagerProvider.instance(executor.getName() + "-pu").em().createQuery("select u from User u left join fetch u.followers where u.id = 1").getSingleResult();
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(1);
            assertThat(user.getTweets()).hasSize(1);
            assertEquals("dbunit rules flat xml example",user.getTweets().get(0).getContent());
            assertThat(user.getFollowers()).isNotNull().hasSize(1);
            Follower expectedFollower = new Follower(2,1);
            assertThat(user.getFollowers()).contains(expectedFollower);
        }

    }

}