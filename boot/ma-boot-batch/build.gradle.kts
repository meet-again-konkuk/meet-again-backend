dependencies {
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    implementation(project(":domain:ma-domain-core"))
    runtimeOnly(project(":config:ma-config-yaml-importer"))
    runtimeOnly(project(":config:ma-config-logging"))
    runtimeOnly(project(":infrastructure:storage:ma-db-core"))
    runtimeOnly(project(":infrastructure:support:ma-file-storage"))
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation(testFixtures(project(":domain:ma-domain-core")))
    testImplementation(project(":infrastructure:storage:ma-db-core"))
    testImplementation("org.jetbrains.exposed:exposed-spring-boot-starter:0.57.0")
    testImplementation("org.jetbrains.exposed:exposed-java-time:0.57.0")
    testRuntimeOnly("com.h2database:h2")
}

