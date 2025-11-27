package uk.gov.moj.cpp.jobstore.springboot.adapters;

import uk.gov.justice.datasource.jobstore.JobStoreDataSourceProvider;

import javax.sql.DataSource;

/**
 * Spring Boot implementation of JobStoreDataSourceProvider.
 * Instead of looking up the datasource via JNDI, it uses the Spring-managed DataSource.
 */
public class SpringJobStoreDataSourceProvider implements JobStoreDataSourceProvider {
    
    private final DataSource dataSource;
    
    public SpringJobStoreDataSourceProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public DataSource getJobStoreDataSource() {
        return dataSource;
    }
}

