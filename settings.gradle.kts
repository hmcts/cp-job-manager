rootProject.name = "cp-job-manager"

include(
    "jobstore-liquibase",
    "jobstore-persistence",
    "jobstore-api",
    "job-executor",
    "job-manager-it"
)

