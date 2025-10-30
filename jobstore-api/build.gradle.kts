dependencies {
    // Compile dependencies
    compileOnly("javax:javaee-api")
    implementation(project(":jobstore-persistence"))

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.hamcrest:hamcrest")
    testImplementation("org.glassfish:javax.json")
}

