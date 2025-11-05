package uk.gov.justice.datasource;

public class DatasourceAcquisitionException extends RuntimeException {

    public DatasourceAcquisitionException(final String message) {
        super(message);
    }

    public DatasourceAcquisitionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
