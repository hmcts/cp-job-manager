dependencies {
    // Compile dependencies
    compileOnly("javax:javaee-api")
    implementation("org.slf4j:slf4j-api")
    implementation(project(":jobstore-api"))
    implementation(project(":jobstore-persistence"))

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.hamcrest:hamcrest")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.apache.tomee:openejb-core")
    testImplementation("org.glassfish:javax.json")
    testImplementation(project(":jobstore-liquibase"))
}

