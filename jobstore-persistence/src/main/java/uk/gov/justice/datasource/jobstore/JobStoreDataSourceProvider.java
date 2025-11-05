package uk.gov.justice.datasource.jobstore;

import javax.sql.DataSource;

public interface JobStoreDataSourceProvider {

    DataSource getJobStoreDataSource();
}
