dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    implementation("com.github.codemonstur:embedded-redis:1.4.0")
    implementation(project(":domain:ma-domain-core"))
}
