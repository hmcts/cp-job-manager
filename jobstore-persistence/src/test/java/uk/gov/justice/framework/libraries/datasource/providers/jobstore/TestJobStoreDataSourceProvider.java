package uk.gov.justice.framework.libraries.datasource.providers.jobstore;

import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import static javax.naming.Context.URL_PKG_PREFIXES;
import static uk.gov.justice.framework.libraries.datasource.providers.jobstore.TestHostProvider.getHost;

import javax.sql.DataSource;

import org.apache.openejb.resource.jdbc.dbcp.BasicDataSource;
import org.postgresql.Driver;
import uk.gov.justice.datasource.jobstore.JobStoreDataSourceProvider;

public class TestJobStoreDataSourceProvider implements JobStoreDataSourceProvider {

    private static final String DEFAULT_PORT = "55432";
    private static final String EVENT_STORE_USER_NAME = "framework";
    private static final String EVENT_STORE_PASSWORD = "framework";

    public DataSource getJobStoreDataSource() {
        // Get port from system property (set by docker-compose plugin) or use default
        String port = System.getProperty("POSTGRES_PORT", DEFAULT_PORT);
        String host = getHost();
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/frameworkjobstore";

        final BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setJdbcDriver(Driver.class.getName());
        basicDataSource.setJdbcUrl(jdbcUrl);
        basicDataSource.setUserName(EVENT_STORE_USER_NAME);
        basicDataSource.setPassword(EVENT_STORE_PASSWORD);

        System.setProperty(INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
        System.setProperty(URL_PKG_PREFIXES, "org.apache.naming");

        return basicDataSource;
    }
}
