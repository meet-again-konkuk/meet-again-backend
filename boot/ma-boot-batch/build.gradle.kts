dependencies {
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    implementation(project(":domain:ma-domain-core"))
    runtimeOnly(project(":config:ma-config-yaml-importer"))
    runtimeOnly(project(":config:ma-config-logging"))
    runtimeOnly(project(":infrastructure:storage:ma-db-core"))
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation(testFixtures(project(":domain:ma-domain-core")))
}
